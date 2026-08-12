package com.michaldrabik.ui_show.sections.seasons.cases

import com.michaldrabik.repository.EpisodesManager
import com.michaldrabik.repository.UserTraktManager
import com.michaldrabik.repository.settings.SettingsRepository
import com.michaldrabik.repository.shows.ShowsRepository
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.SeasonBundle
import com.michaldrabik.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsWatchedSeasonCase @Inject constructor(
  private val showsRepository: ShowsRepository,
  private val settingsRepository: SettingsRepository,
  private val episodesManager: EpisodesManager,
  private val userManager: UserTraktManager,
) {

  suspend fun setSeasonWatched(
    show: Show,
    season: Season,
    isChecked: Boolean,
    isLocal: Boolean,
    customDate: ZonedDateTime?,
  ): Result {
    val bundle = SeasonBundle(season, show)

    val isMyShows = showsRepository.myShows.exists(show.ids.tmdb)
    val isWatchlist = showsRepository.watchlistShows.exists(show.ids.tmdb)
    val isHidden = showsRepository.hiddenShows.exists(show.ids.tmdb)
    val isCollection = isMyShows || isWatchlist || isHidden

    when {
      isChecked -> {
        val episodesAdded = episodesManager.setSeasonWatched(bundle, customDate)
        if (isMyShows) {
        }
        return Result.SUCCESS
      }
      else -> {
        episodesManager.setSeasonUnwatched(bundle)

        val traktQuickRemoveEnabled = settingsRepository.load().traktQuickRemoveEnabled
        val showRemoveTrakt = userManager.isAuthorized() && traktQuickRemoveEnabled && !isLocal && isCollection
        if (showRemoveTrakt) {
          return Result.REMOVE_FROM_TRAKT
        }
        return Result.SUCCESS
      }
    }
  }

  enum class Result {
    SUCCESS,
    REMOVE_FROM_TRAKT,
  }
}
