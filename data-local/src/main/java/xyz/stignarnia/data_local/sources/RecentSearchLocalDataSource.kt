package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.RecentSearch

interface RecentSearchLocalDataSource {

  suspend fun getAll(limit: Int): List<RecentSearch>

  suspend fun upsert(searches: List<RecentSearch>)

  suspend fun deleteAll()
}
