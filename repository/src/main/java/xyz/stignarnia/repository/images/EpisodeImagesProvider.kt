package xyz.stignarnia.repository.images

import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageFamily.EPISODE
import xyz.stignarnia.uiModel.ImageSource
import xyz.stignarnia.uiModel.ImageStatus.AVAILABLE
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.ImageType.FANART
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeImagesProvider
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
  ) {
    suspend fun loadRemoteImage(
      showId: IdTmdb,
      episode: Episode,
    ): Image =
      withContext(dispatchers.IO) {
        val cachedImage = findCachedImage(episode, FANART)
        if (cachedImage.status == AVAILABLE) {
          return@withContext cachedImage
        }

        var image = Image.createUnavailable(FANART)
        try {
          var remoteImage = remoteSource.tmdb.fetchEpisodeImage(showId.id, episode.season, episode.number)
          if (remoteImage == null && (episode.numberAbs ?: 0) > 0) {
            // Try absolute episode number if present (may happen with certain Anime series)
            remoteImage = remoteSource.tmdb.fetchEpisodeImage(showId.id, episode.season, episode.numberAbs)
          }
          image =
            when (remoteImage) {
              null -> {
                Image.createUnavailable(FANART)
              }

              else -> {
                Image(
                  id = -1,
                  idTvdb = episode.ids.tvdb,
                  idTmdb = episode.ids.tmdb,
                  type = FANART,
                  family = EPISODE,
                  fileUrl = remoteImage.file_path,
                  thumbnailUrl = "",
                  status = AVAILABLE,
                  source = ImageSource.TMDB,
                )
              }
            }
        } catch (error: Throwable) {
          Timber.w(error)
        }

        when (image.status) {
          UNAVAILABLE -> {
            localSource.showImages.deleteByEpisodeId(
              id = episode.ids.tmdb.id,
              type = image.type.key,
            )
          }

          else -> {
            localSource.showImages.insertEpisodeImage(mappers.image.toDatabaseShow(image))
          }
        }

        return@withContext image
      }

    private suspend fun findCachedImage(
      episode: Episode,
      type: ImageType,
    ): Image =
      when (val image = localSource.showImages.getByEpisodeId(episode.ids.tmdb.id, type.key)) {
        null -> Image.createUnknown(type, EPISODE)
        else -> mappers.image.fromDatabase(image).copy(type = type)
      }
  }
