package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.dataLocal.database.model.MovieStreaming
import xyz.stignarnia.dataLocal.sources.MovieStreamingsLocalDataSource

@Dao
interface MovieStreamingsDao :
  BaseDao<MovieStreaming>,
  MovieStreamingsLocalDataSource {
  @Transaction
  override suspend fun replace(
    tmdbId: Long,
    entities: List<MovieStreaming>,
  ) {
    deleteById(tmdbId)
    insert(entities)
  }

  @Query("SELECT * FROM movies_streamings WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): List<MovieStreaming>

  @Query("DELETE FROM movies_streamings WHERE id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)

  @Query("DELETE FROM movies_streamings")
  override suspend fun deleteAll()
}
