package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.Movie
import xyz.stignarnia.data_local.database.model.MovieCollectionItem

interface MovieCollectionsItemsLocalDataSource {

  suspend fun getById(collectionId: Long): List<Movie>

  suspend fun deleteById(collectionId: Long)

  suspend fun replace(
    collectionId: Long,
    items: List<MovieCollectionItem>,
  )
}
