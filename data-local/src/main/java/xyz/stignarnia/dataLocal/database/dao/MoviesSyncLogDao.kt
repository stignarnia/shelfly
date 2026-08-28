package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.dataLocal.database.model.MoviesSyncLog
import xyz.stignarnia.dataLocal.sources.MoviesSyncLogLocalDataSource

@Dao
interface MoviesSyncLogDao : MoviesSyncLogLocalDataSource {
  @Query("SELECT * from sync_movies_log")
  override suspend fun getAll(): List<MoviesSyncLog>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun upsert(log: MoviesSyncLog)
}
