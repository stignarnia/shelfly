package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.TranslationsSyncLog

interface TranslationsShowsSyncLogLocalDataSource {

  suspend fun getAll(): List<TranslationsSyncLog>

  suspend fun getById(idTmdb: Long): TranslationsSyncLog?

  suspend fun upsert(log: TranslationsSyncLog)

  suspend fun deleteAll()
}
