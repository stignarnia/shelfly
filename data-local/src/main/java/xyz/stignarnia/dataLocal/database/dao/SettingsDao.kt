package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.dataLocal.database.model.Settings
import xyz.stignarnia.dataLocal.sources.SettingsLocalDataSource

@Dao
interface SettingsDao : SettingsLocalDataSource {
  @Query("SELECT * FROM settings")
  override suspend fun getAll(): Settings

  @Query("SELECT COUNT(*) FROM settings")
  override suspend fun getCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun upsert(settings: Settings)
}
