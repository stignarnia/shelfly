package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.Settings

interface SettingsLocalDataSource {

  suspend fun getAll(): Settings

  suspend fun getCount(): Int

  suspend fun upsert(settings: Settings)
}
