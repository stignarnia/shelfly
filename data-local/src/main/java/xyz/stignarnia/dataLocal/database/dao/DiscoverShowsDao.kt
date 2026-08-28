package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.dataLocal.database.model.DiscoverShow
import xyz.stignarnia.dataLocal.sources.DiscoverShowsLocalDataSource

@Dao
interface DiscoverShowsDao : DiscoverShowsLocalDataSource {
  @Query("SELECT * FROM shows_discover ORDER BY id")
  override suspend fun getAll(): List<DiscoverShow>

  @Query("SELECT * from shows_discover ORDER BY created_at DESC LIMIT 1")
  override suspend fun getMostRecent(): DiscoverShow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun upsert(shows: List<DiscoverShow>)

  @Query("DELETE FROM shows_discover")
  override suspend fun deleteAll()

  @Transaction
  override suspend fun replace(shows: List<DiscoverShow>) {
    deleteAll()
    upsert(shows)
  }
}
