package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.data_local.database.model.Settings
import xyz.stignarnia.data_local.sources.SettingsLocalDataSource

@Dao
interface SettingsDao : SettingsLocalDataSource {

  @Query("SELECT * FROM settings")
  override suspend fun getAll(): Settings

  @Query("SELECT COUNT(*) FROM settings")
  override suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun upsert(settings: Settings)
}
