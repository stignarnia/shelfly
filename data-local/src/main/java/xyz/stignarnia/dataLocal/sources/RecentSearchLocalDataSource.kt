package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.RecentSearch

interface RecentSearchLocalDataSource {
  suspend fun getAll(limit: Int): List<RecentSearch>

  suspend fun upsert(searches: List<RecentSearch>)

  suspend fun deleteAll()
}
