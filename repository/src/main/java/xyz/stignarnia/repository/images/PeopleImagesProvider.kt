package xyz.stignarnia.repository.images

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.PersonImage
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageFamily
import xyz.stignarnia.ui_model.ImageSource
import xyz.stignarnia.ui_model.ImageType
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleImagesProvider @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val remoteSource: RemoteDataSource,
) {

  suspend fun loadCachedImage(personTmdbId: IdTmdb): Image? =
    withContext(dispatchers.IO) {
      val localPerson = localSource.people.getById(personTmdbId.id)
      return@withContext localPerson?.image?.let {
        Image.createAvailable(
          ids = Ids.EMPTY,
          type = ImageType.PROFILE,
          family = ImageFamily.PROFILE,
          path = it,
          source = ImageSource.TMDB,
        )
      }
    }

  suspend fun loadImages(personTmdbId: IdTmdb): List<Image> =
    withContext(dispatchers.IO) {
      val localTimestamp = localSource.peopleImages.getTimestampForPerson(personTmdbId.id) ?: 0
      if (localTimestamp + Config.PEOPLE_IMAGES_CACHE_DURATION > nowUtcMillis()) {
        val local = localSource.peopleImages.getAll(personTmdbId.id)
        return@withContext local.map {
          Image.createAvailable(
            ids = Ids.EMPTY,
            type = ImageType.PROFILE,
            family = ImageFamily.PROFILE,
            path = it.filePath,
            source = ImageSource.TMDB,
          )
        }
      }

      val images = (remoteSource.tmdb.fetchPersonImages(personTmdbId.id).profiles ?: emptyList())
        .filter { it.file_path.isNotBlank() }
      val dbImages = images.map {
        PersonImage(
          id = 0,
          idTmdb = personTmdbId.id,
          filePath = it.file_path,
          createdAt = nowUtc(),
          updatedAt = nowUtc(),
        )
      }

      localSource.peopleImages.insertSingle(personTmdbId.id, dbImages)

      return@withContext images.map {
        Image.createAvailable(
          ids = Ids.EMPTY,
          type = ImageType.PROFILE,
          family = ImageFamily.PROFILE,
          path = it.file_path,
          source = ImageSource.TMDB,
        )
      }
    }
}
