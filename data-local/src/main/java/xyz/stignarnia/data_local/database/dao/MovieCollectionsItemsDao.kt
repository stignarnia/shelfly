
package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.data_local.database.model.Movie
import xyz.stignarnia.data_local.database.model.MovieCollectionItem
import xyz.stignarnia.data_local.sources.MovieCollectionsItemsLocalDataSource

@Dao
interface MovieCollectionsItemsDao :
  BaseDao<MovieCollectionItem>,
  MovieCollectionsItemsLocalDataSource {

  @Query(
    """
    SELECT
    movies.id_tmdb,
    movies.id_tmdb,
    movies.id_imdb,
    movies.id_slug,
    movies.title,
    movies.year,
    movies.overview,
    movies.released,
    movies.runtime,
    movies.country,
    movies.trailer,
    movies.language,
    movies.homepage,
    movies.status,
    movies.rating,
    movies.votes,
    movies.comment_count,
    movies.genres,
    movies_collections_items.updated_at AS updated_at,
    movies_collections_items.created_at AS created_at
    FROM movies INNER JOIN movies_collections_items USING(id_tmdb)
    WHERE id_tmdb_collection == :collectionId
    ORDER BY rank ASC""",
  )
  override suspend fun getById(collectionId: Long): List<Movie>

  @Transaction
  override suspend fun replace(
    collectionId: Long,
    items: List<MovieCollectionItem>,
  ) {
    deleteById(collectionId)
    insert(items)
  }

  @Query("DELETE FROM movies_collections_items WHERE id_tmdb_collection == :collectionId")
  override suspend fun deleteById(collectionId: Long)
}
