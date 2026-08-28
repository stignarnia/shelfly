package xyz.stignarnia.uiMyShows.watchlist.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiModel.UpcomingFilter
import javax.inject.Inject

@ViewModelScoped
class WatchlistFiltersCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun toggleUpcomingFilter() {
      val current = settingsRepository.filters.watchlistShowsUpcoming
      settingsRepository.filters.watchlistShowsUpcoming =
        when (current) {
          UpcomingFilter.OFF -> UpcomingFilter.UPCOMING
          UpcomingFilter.UPCOMING -> UpcomingFilter.RELEASED
          UpcomingFilter.RELEASED -> UpcomingFilter.OFF
        }
    }
  }
