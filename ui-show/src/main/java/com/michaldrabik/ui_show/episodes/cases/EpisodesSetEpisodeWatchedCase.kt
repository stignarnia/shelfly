package com.michaldrabik.ui_show.episodes.cases

import com.michaldrabik.repository.EpisodesManager
import com.michaldrabik.repository.UserTraktManager
import com.michaldrabik.repository.settings.SettingsRepository
import com.michaldrabik.repository.shows.ShowsRepository
import com.michaldrabik.ui_model.EpisodeBundle
import com.michaldrabik.ui_show.sections.seasons.helpers.SeasonsCache
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class EpisodesSetEpisodeWatchedCase @Inject constructor(
  private val showsRepository: ShowsRepository,
  private val episodesManager: EpisodesManager,
  private val userTraktManager: UserTraktManager,
  private val seasonsCache: SeasonsCache,
  private val settingsRepository: SettingsRepository,
) {

  suspend fun setEpisodeWatched(
    episodeBundle: EpisodeBundle,
    isChecked: Boolean,
    customDate: ZonedDateTime?,
  ): Result {
    val (episode, _, show) = episodeBundle

    val isMyShows = showsRepository.myShows.exists(show.ids.tmdb)
    val isWatchlist = showsRepository.watchlistShows.exists(show.ids.tmdb)
    val isHidden = showsRepository.hiddenShows.exists(show.ids.tmdb)
    val isCollection = isMyShows || isWatchlist || isHidden

    when {
      isChecked -> {
        episodesManager.setEpisodeWatched(episodeBundle, customDate)
        if (isMyShows) {
        }
        return Result.SUCCESS
      }
      else -> {
        episodesManager.setEpisodeUnwatched(episodeBundle)

        val traktQuickRemoveEnabled = settingsRepository.load().traktQuickRemoveEnabled
        val isSeasonLocal = seasonsCache.areSeasonsLocal(show.ids.tmdb)

        val showRemoveTrakt = userTraktManager.isAuthorized() &&
          traktQuickRemoveEnabled &&
          !isSeasonLocal &&
          isCollection

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
