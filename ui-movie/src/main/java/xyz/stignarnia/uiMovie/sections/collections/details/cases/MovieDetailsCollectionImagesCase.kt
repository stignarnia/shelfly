package xyz.stignarnia.uiMovie.sections.collections.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiMovie.sections.collections.details.recycler.MovieDetailsCollectionItem.MovieItem
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsCollectionImagesCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val movieImagesProvider: MovieImagesProvider,
  ) {
    suspend fun loadMissingImage(
      item: MovieItem,
      force: Boolean,
    ): MovieItem =
      withContext(dispatchers.IO) {
        try {
          val image = movieImagesProvider.loadRemoteImage(item.movie, item.image.type, force)
          return@withContext item.copy(image = image)
        } catch (error: Throwable) {
          Timber.w(error)
          return@withContext item.copy(image = Image.createUnavailable(item.image.type))
        }
      }
  }
