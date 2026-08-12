package xyz.stignarnia.ui_my_movies.watchlist.cases

import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_model.UpcomingFilter
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class WatchlistFiltersCase @Inject constructor(
  private val settingsRepository: SettingsRepository,
) {

  fun toggleUpcomingFilter() {
    val current = settingsRepository.filters.watchlistMoviesUpcoming
    settingsRepository.filters.watchlistMoviesUpcoming = when (current) {
      UpcomingFilter.OFF -> UpcomingFilter.UPCOMING
      UpcomingFilter.UPCOMING -> UpcomingFilter.RELEASED
      UpcomingFilter.RELEASED -> UpcomingFilter.OFF
    }
  }
}
