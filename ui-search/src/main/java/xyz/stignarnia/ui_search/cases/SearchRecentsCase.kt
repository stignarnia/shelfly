package xyz.stignarnia.ui_search.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.ui_model.RecentSearch
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject
import xyz.stignarnia.data_local.database.model.RecentSearch as RecentSearchDb

@ViewModelScoped
class SearchRecentsCase @Inject constructor(
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
