package xyz.stignarnia.uiSearch.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.uiModel.RecentSearch
import javax.inject.Inject
import xyz.stignarnia.dataLocal.database.model.RecentSearch as RecentSearchDb

@ViewModelScoped
class SearchRecentsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
  ) {
    suspend fun getRecentSearches(limit: Int): List<RecentSearch> =
      withContext(dispatchers.IO) {
        localSource.recentSearch
          .getAll(limit)
          .map { RecentSearch(it.text) }
      }

    suspend fun clearRecentSearches() =
      withContext(dispatchers.IO) {
        localSource.recentSearch.deleteAll()
      }

    suspend fun saveRecentSearch(query: String) =
      withContext(dispatchers.IO) {
        val now = nowUtcMillis()
        localSource.recentSearch.upsert(listOf(RecentSearchDb(0, query, now, now)))
      }
  }
