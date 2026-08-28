package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.TranslationsMoviesSyncLog

interface TranslationsMoviesSyncLogLocalDataSource {
  suspend fun getAll(): List<TranslationsMoviesSyncLog>

  suspend fun getById(idTmdb: Long): TranslationsMoviesSyncLog?

  suspend fun upsert(log: TranslationsMoviesSyncLog)

  suspend fun deleteAll()
}
