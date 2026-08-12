package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.TranslationsMoviesSyncLog

interface TranslationsMoviesSyncLogLocalDataSource {

  suspend fun getAll(): List<TranslationsMoviesSyncLog>

  suspend fun getById(idTmdb: Long): TranslationsMoviesSyncLog?

  suspend fun upsert(log: TranslationsMoviesSyncLog)

  suspend fun deleteAll()
}
