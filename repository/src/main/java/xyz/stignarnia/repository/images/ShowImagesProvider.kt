package xyz.stignarnia.repository.images

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.data_remote.tmdb.model.TmdbImage
import xyz.stignarnia.data_remote.tmdb.model.TmdbImages
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.IdTvdb
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageFamily.SHOW
import xyz.stignarnia.ui_model.ImageSource.CUSTOM
import xyz.stignarnia.ui_model.ImageSource.TMDB
import xyz.stignarnia.ui_model.ImageStatus.AVAILABLE
import xyz.stignarnia.ui_model.ImageStatus.UNAVAILABLE
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.ImageType.FANART
import xyz.stignarnia.ui_model.ImageType.FANART_WIDE
import xyz.stignarnia.ui_model.ImageType.POSTER
import xyz.stignarnia.ui_model.Show
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowImagesProvider @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
  private var translationsRepository: TranslationsRepository,
) {

  private val unavailableCache = mutableSetOf<IdTmdb>()

  suspend fun findCachedImage(
    show: Show,
    type: ImageType,
  ): Image =
    withContext(dispatchers.IO) {
      val image = localSource.showImages.getByShowId(show.ids.tmdb.id, type.key)
      when (image) {
        null -> {
          if (unavailableCache.contains(show.ids.tmdb)) {
            Image.createUnavailable(type, SHOW)
          } else {
            Image.createUnknown(type, SHOW)
          }
        }
        else -> {
          mappers.image.fromDatabase(image).copy(type = type)
        }
      }
    }

  suspend fun loadRemoteImage(
    show: Show,
    type: ImageType,
    force: Boolean = false,
  ): Image =
    withContext(dispatchers.IO) {
      val tvdbId = show.ids.tvdb
      val tmdbId = show.ids.tmdb

      val cachedImage = findCachedImage(show, type)
      if (cachedImage.status in arrayOf(AVAILABLE, UNAVAILABLE)) {
        if (!force || cachedImage.source == CUSTOM) {
          return@withContext cachedImage
        }
      }

      var source = TMDB
      val images = remoteSource.tmdb.fetchShowImages(tmdbId.id)

      var typeImages = when (type) {
        POSTER -> images.posters ?: emptyList()
        FANART, FANART_WIDE -> images.backdrops ?: emptyList()
        else -> throw Error("Invalid type")
      }
      // If requested poster is unavailable try backing up to a fanart
      if (typeImages.isEmpty() && type == POSTER) {
        typeImages = images.backdrops ?: emptyList()
      }

      if (typeImages.isEmpty() && type in arrayOf(FANART, FANART_WIDE)) {
        run {
          // If requested fanart is unavailable try backing up to an episode image
          val seasons = remoteSource.tmdb.fetchSeasons(show.tmdbId)
          if (seasons.isNotEmpty()) {
            val episode = seasons[0].episodes?.firstOrNull()
            episode?.let { ep ->
              runCatching {
                val backupImage = remoteSource.tmdb.fetchEpisodeImage(tmdbId.id, ep.season, ep.number)
                backupImage?.let {
                  typeImages = listOf(TmdbImage(it.file_path, 0F, 0, "en"))
                }
              }
            }
          }
        }
      }

      val remoteImage = findBestImage(typeImages, type)
      val image = when (remoteImage) {
        null -> Image.createUnavailable(type)
        else -> Image.createAvailable(show.ids, type, SHOW, remoteImage.file_path, source)
      }

      when (image.status) {
        UNAVAILABLE -> {
          unavailableCache.add(show.ids.tmdb)
          localSource.showImages.deleteByShowId(tmdbId.id, image.type.key)
        }
        else -> {
          localSource.showImages.insertShowImage(mappers.image.toDatabaseShow(image))
          saveExtraImage(tmdbId, tvdbId, images, type)
        }
      }

      image
    }

  private suspend fun saveExtraImage(
    tmdbId: IdTmdb,
    tvdbId: IdTvdb,
    images: TmdbImages,
    targetType: ImageType,
  ) {
    val extraType = if (targetType == POSTER) FANART else POSTER
    val typeImages = when (extraType) {
      POSTER -> images.posters ?: emptyList()
      FANART, FANART_WIDE -> images.backdrops ?: emptyList()
      else -> throw Error("Invalid type")
    }
    findBestImage(typeImages, extraType)?.let {
      val extraImage = Image(-1, tvdbId, tmdbId, extraType, SHOW, it.file_path, "", AVAILABLE, TMDB)
      localSource.showImages.insertShowImage(mappers.image.toDatabaseShow(extraImage))
    }
  }

  suspend fun loadRemoteImages(
    show: Show,
    type: ImageType,
  ): List<Image> =
    withContext(dispatchers.IO) {
      val tmdbId = show.ids.tmdb
      val remoteImages = remoteSource.tmdb.fetchShowImages(tmdbId.id)
      val typeImages = when (type) {
        POSTER -> remoteImages.posters ?: emptyList()
        FANART, FANART_WIDE -> remoteImages.backdrops ?: emptyList()
        else -> throw Error("Invalid type")
      }
      typeImages
        .map {
          Image.createAvailable(show.ids, type, SHOW, it.file_path, TMDB)
        }
    }

  private fun findBestImage(
    images: List<TmdbImage>,
    type: ImageType,
  ): TmdbImage? {
    val language = translationsRepository.getLanguage()
    val comparator = when (type) {
      POSTER -> compareBy<TmdbImage> { it.isLanguage(language) }
        .thenBy { it.isEnglish() }
        .thenBy { it.isPlain() }
      else -> compareBy<TmdbImage> { it.isPlain() }
        .thenBy { it.isLanguage(language) }
        .thenBy { it.isEnglish() }
    }
    return images.maxWithOrNull(comparator.thenBy { it.getVoteScore() })
  }

  suspend fun deleteLocalCache() =
    withContext(dispatchers.IO) {
      localSource.showImages.deleteAll()
    }

  fun clear() = unavailableCache.clear()
}
