package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.data_local.database.model.TranslationsMoviesSyncLog
import xyz.stignarnia.data_local.sources.TranslationsMoviesSyncLogLocalDataSource

@Dao
interface TranslationsMoviesSyncLogDao : TranslationsMoviesSyncLogLocalDataSource {

  @Query("SELECT * from sync_movies_translations_log")
  override suspend fun getAll(): List<TranslationsMoviesSyncLog>

  @Query("SELECT * from sync_movies_translations_log WHERE id_movie_tmdb == :idTmdb")
  override suspend fun getById(idTmdb: Long): TranslationsMoviesSyncLog?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun upsert(log: TranslationsMoviesSyncLog)

  @Query("DELETE FROM sync_movies_translations_log")
  override suspend fun deleteAll()
}
