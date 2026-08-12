package xyz.stignarnia.ui_movie.sections.collections.details.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.movies.MovieCollectionsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_movie.sections.collections.details.recycler.MovieDetailsCollectionItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsCollectionDetailsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val collectionsRepository: MovieCollectionsRepository,
) {

  suspend fun loadCollection(collectionId: IdTmdb): MovieDetailsCollectionItem.HeaderItem =
    withContext(dispatchers.IO) {
      val collection = collectionsRepository.loadCollection(collectionId)
        ?: throw Error("Requested collection must be available at this point")

      return@withContext MovieDetailsCollectionItem.HeaderItem(
        title = collection.name,
        description = collection.description,
      )
    }
}
