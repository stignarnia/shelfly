package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.Settings

interface SettingsLocalDataSource {
  suspend fun getAll(): Settings

  suspend fun getCount(): Int

  suspend fun upsert(settings: Settings)
}
