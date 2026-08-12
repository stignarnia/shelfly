package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.data_local.sources.SeasonsLocalDataSource

@Dao
interface SeasonsDao : SeasonsLocalDataSource {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(items: List<Season>): List<Long>

  @Update(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun update(items: List<Season>)

  @Delete
  override suspend fun delete(items: List<Season>)

  @Query("SELECT * FROM seasons WHERE id_tmdb IN (:tmdbIds)")
  override suspend fun getAll(tmdbIds: List<Long>): List<Season>

  @Transaction
  override suspend fun getAllByShowsIds(tmdbIds: List<Long>): List<Season> {
    val result = mutableListOf<Season>()
    val chunks = tmdbIds.chunked(50)
    chunks.forEach { chunk ->
      result += getAllByShowsIdsChunk(chunk)
    }
    return result
  }

  @Query("SELECT * FROM seasons WHERE id_show_tmdb IN (:tmdbIds)")
  override suspend fun getAllByShowsIdsChunk(tmdbIds: List<Long>): List<Season>

  @Query("SELECT * FROM seasons WHERE is_watched = 1")
  override suspend fun getAllWatched(): List<Season>

  @Query("SELECT * FROM seasons WHERE id_show_tmdb IN (:tmdbIds) AND is_watched = 1")
  override suspend fun getAllWatchedForShows(tmdbIds: List<Long>): List<Season>

  @Query("SELECT id_tmdb FROM seasons WHERE id_show_tmdb IN (:tmdbIds) AND is_watched = 1")
  override suspend fun getAllWatchedIdsForShows(tmdbIds: List<Long>): List<Long>

  @Query("SELECT * FROM seasons WHERE id_show_tmdb = :tmdbId")
  override suspend fun getAllByShowId(tmdbId: Long): List<Season>

  @Query("SELECT * FROM seasons WHERE id_tmdb = :tmdbId")
  override suspend fun getById(tmdbId: Long): Season?

  @Transaction
  override suspend fun upsert(items: List<Season>) {
    val result = insert(items)
    val updateList = mutableListOf<Season>()

    result.forEachIndexed { index, id ->
      if (id == -1L) updateList.add(items[index])
    }

    if (updateList.isNotEmpty()) update(updateList)
  }

  @Query("DELETE FROM seasons WHERE id_show_tmdb = :showTmdbId")
  override suspend fun deleteAllForShow(showTmdbId: Long)
}
