package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.dataLocal.database.model.RelatedMovie
import xyz.stignarnia.dataLocal.sources.RelatedMoviesLocalDataSource

@Dao
interface RelatedMoviesDao : RelatedMoviesLocalDataSource {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  override suspend fun insert(items: List<RelatedMovie>): List<Long>

  @Query("SELECT * FROM movies_related WHERE id_tmdb_related_movie == :tmdbId")
  override suspend fun getAllById(tmdbId: Long): List<RelatedMovie>

  @Query("SELECT * FROM movies_related")
  override suspend fun getAll(): List<RelatedMovie>

  @Query("DELETE FROM movies_related WHERE id_tmdb_related_movie == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)
}
