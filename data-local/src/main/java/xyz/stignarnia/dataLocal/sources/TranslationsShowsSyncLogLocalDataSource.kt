package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.TranslationsSyncLog

interface TranslationsShowsSyncLogLocalDataSource {
  suspend fun getAll(): List<TranslationsSyncLog>

  suspend fun getById(idTmdb: Long): TranslationsSyncLog?

  suspend fun upsert(log: TranslationsSyncLog)

  suspend fun deleteAll()
}
