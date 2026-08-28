package xyz.stignarnia.uiDiscover.cases

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.common.AppScopeProvider
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiModel.DiscoverFilters
import javax.inject.Inject

@ViewModelScoped
class DiscoverFiltersCase
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val settingsRepository: SettingsRepository,
  ) {
    suspend fun loadFilters(): DiscoverFilters =
      withContext(dispatchers.IO) {
        val settings = settingsRepository.load()
        DiscoverFilters(
          feedOrder = settingsRepository.filters.discoverShowsFeed,
          hideAnticipated = !settings.showAnticipatedShows,
          hideCollection = !settings.showCollectionShows,
          genres = settings.discoverFilterGenres.toList(),
          providers = settingsRepository.filters.discoverShowsProviders,
        )
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
              settingsRepository.filters.discoverShowsFeed = initial.feedOrder
              settingsRepository.filters.discoverShowsProviders = initial.providers
              settingsRepository.update(
                settings.copy(
                  discoverFilterGenres = initial.genres,
                  showAnticipatedShows = !initial.hideAnticipated,
                  showCollectionShows = !initial.hideCollection,
                ),
              )
            }
          }
        } catch (error: Throwable) {
          rethrowCancellation(error)
        }
      }
    }

    suspend fun toggleCollection() {
      withContext(dispatchers.IO) {
        val settings = settingsRepository.load()
        settingsRepository.update(
          settings.copy(showCollectionShows = !settings.showCollectionShows),
        )
      }
    }
  }
