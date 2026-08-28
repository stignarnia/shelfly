package xyz.stignarnia.uiShow.episodes.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiShow.sections.seasons.helpers.SeasonsCache
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class EpisodesSetEpisodeWatchedCase
  @Inject
  constructor(
    private val showsRepository: ShowsRepository,
    private val episodesManager: EpisodesManager,
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

          return Result.SUCCESS
        }
      }
    }

    enum class Result {
      SUCCESS,
    }
  }
