package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.data_local.database.model.RelatedShow
import xyz.stignarnia.data_local.sources.RelatedShowsLocalDataSource

@Dao
interface RelatedShowsDao : RelatedShowsLocalDataSource {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  override suspend fun insert(items: List<RelatedShow>): List<Long>

  @Query("SELECT * FROM shows_related WHERE id_tmdb_related_show == :tmdbId")
  override suspend fun getAllById(tmdbId: Long): List<RelatedShow>

  @Query("SELECT * FROM shows_related")
  override suspend fun getAll(): List<RelatedShow>

  @Query("DELETE FROM shows_related WHERE id_tmdb_related_show == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)
}
