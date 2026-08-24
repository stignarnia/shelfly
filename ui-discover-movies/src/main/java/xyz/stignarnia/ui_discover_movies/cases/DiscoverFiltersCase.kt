package xyz.stignarnia.ui_discover_movies.cases

import android.content.Context
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.common.AppScopeProvider
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_model.DiscoverFilters
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class DiscoverFiltersCase @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val dispatchers: CoroutineDispatchers,
  private val settingsRepository: SettingsRepository,
) {

  suspend fun loadFilters(): DiscoverFilters =
    withContext(dispatchers.IO) {
      val settings = settingsRepository.load()
      DiscoverFilters(
        feedOrder = settingsRepository.filters.discoverMoviesFeed,
        hideAnticipated = !settings.showAnticipatedMovies,
        hideCollection = !settings.showCollectionMovies,
        genres = settings.discoverMoviesFilterGenres.toList(),
        providers = settingsRepository.filters.discoverMoviesProviders,
      )
    }

  suspend fun toggleCollection() {
    withContext(dispatchers.IO) {
      val settings = settingsRepository.load()
      settingsRepository.update(
        settings.copy(showCollectionMovies = !settings.showCollectionMovies),
      )
    }
  }

  fun revertFilters(
    initialFilters: DiscoverFilters?,
    currentFilters: DiscoverFilters?,
  ) {
    (context as AppScopeProvider).appScope.launch {
      try {
        if (initialFilters != currentFilters) {
          initialFilters?.let { initial ->
            val settings = settingsRepository.load()
            settingsRepository.filters.discoverMoviesFeed = initial.feedOrder
            settingsRepository.filters.discoverMoviesProviders = initial.providers
            settingsRepository.update(
              settings.copy(
                discoverMoviesFilterGenres = initial.genres,
                showAnticipatedMovies = !initial.hideAnticipated,
                showCollectionMovies = !initial.hideCollection,
              ),
            )
          }
        }
      } catch (error: Throwable) {
        rethrowCancellation(error)
      }
    }
  }
}
