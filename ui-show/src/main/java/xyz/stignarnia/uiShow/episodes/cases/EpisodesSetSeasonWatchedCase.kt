package xyz.stignarnia.uiShow.episodes.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.SeasonBundle
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiShow.sections.seasons.helpers.SeasonsCache
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class EpisodesSetSeasonWatchedCase
  @Inject
  constructor(
    private val showsRepository: ShowsRepository,
    private val episodesManager: EpisodesManager,
    private val seasonsCache: SeasonsCache,
    private val settingsRepository: SettingsRepository,
  ) {
    suspend fun setSeasonWatched(
      show: Show,
      season: Season,
      isChecked: Boolean,
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

          return Result.SUCCESS
        }
      }
    }

    enum class Result {
      SUCCESS,
    }
  }
