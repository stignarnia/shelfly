package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.Movie
import xyz.stignarnia.dataLocal.database.model.MovieCollectionItem

interface MovieCollectionsItemsLocalDataSource {
  suspend fun getById(collectionId: Long): List<Movie>

  suspend fun deleteById(collectionId: Long)

  suspend fun replace(
    collectionId: Long,
    items: List<MovieCollectionItem>,
  )
}
