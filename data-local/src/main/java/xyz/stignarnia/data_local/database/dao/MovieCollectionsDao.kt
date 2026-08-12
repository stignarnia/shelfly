package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.data_local.database.model.MovieCollection
import xyz.stignarnia.data_local.sources.MovieCollectionsLocalDataSource

@Dao
interface MovieCollectionsDao :
  BaseDao<MovieCollection>,
  MovieCollectionsLocalDataSource {

  @Query("SELECT * FROM movies_collections WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): MovieCollection?

  @Query("SELECT * FROM movies_collections WHERE id_tmdb_movie == :movieTmdbId")
  override suspend fun getByMovieId(movieTmdbId: Long): List<MovieCollection>

  @Transaction
  override suspend fun replaceByMovieId(
    movieTmdbId: Long,
    entities: List<MovieCollection>,
  ) {
    val deleteCollections = getByMovieId(movieTmdbId).map { it.idTmdb }

    deleteCollectionsItems(deleteCollections)
    deleteCollections(deleteCollections)

    insert(entities)
  }

  override suspend fun insertAll(items: List<MovieCollection>) {
    insert(items)
  }

  @Query("DELETE FROM movies_collections WHERE id_tmdb IN (:collectionIds)")
  suspend fun deleteCollections(collectionIds: List<Long>)

  @Query("DELETE FROM movies_collections_items WHERE id_tmdb_collection IN (:collectionIds)")
  suspend fun deleteCollectionsItems(collectionIds: List<Long>)
}
