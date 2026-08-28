package xyz.stignarnia.uiShow.sections.seasons.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.network.NetworkStatusProvider
import xyz.stignarnia.uiModel.RatingState
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiShow.episodes.recycler.EpisodeListItem
import xyz.stignarnia.uiShow.sections.seasons.helpers.SeasonsBundle
import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonListItem
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsLoadSeasonsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
    private val showsRepository: ShowsRepository,
    private val settingsRepository: SettingsRepository,
    private val ratingsRepository: RatingsRepository,
    private val translationsRepository: TranslationsRepository,
    private val episodesManager: EpisodesManager,
    private val dateFormatProvider: DateFormatProvider,
    private val networkStatusProvider: NetworkStatusProvider,
  ) {
    suspend fun loadSeasons(show: Show): SeasonsBundle =
      withContext(dispatchers.IO) {
        val showSpecialSeasons = settingsRepository.load().specialSeasonsEnabled
        try {
          if (!networkStatusProvider.isOnline()) {
            loadLocalSeasons(show, showSpecialSeasons)
          }

          val remoteSeasons =
            remoteSource.tmdb
              .fetchSeasons(show.tmdbId)
              .map { mappers.season.fromNetwork(it) }
              .filter { it.episodes.isNotEmpty() }
              .filter { if (!showSpecialSeasons) !it.isSpecial() else true }

          val isFollowed = showsRepository.myShows.load(show.ids.tmdb) != null
          if (isFollowed) {
            episodesManager.invalidateSeasons(show, remoteSeasons)
          }

          val seasonsItems = mapToSeasonItems(remoteSeasons, show)
          SeasonsBundle(seasonsItems, isLocal = false)
        } catch (error: Throwable) {
          loadLocalSeasons(show, showSpecialSeasons)
        }
      }

    private suspend fun loadLocalSeasons(
      show: Show,
      showSpecials: Boolean,
    ): SeasonsBundle {
      val localEpisodes = localSource.episodes.getAllByShowId(show.tmdbId)
      val localSeasons =
        localSource.seasons
          .getAllByShowId(show.tmdbId)
          .map { season ->
            val seasonEpisodes = localEpisodes.filter { ep -> ep.idSeason == season.idTmdb }
            mappers.season.fromDatabase(season, seasonEpisodes)
          }.filter { it.episodes.isNotEmpty() }
          .filter { if (!showSpecials) !it.isSpecial() else true }

      val seasonsItems = mapToSeasonItems(localSeasons, show)
      return SeasonsBundle(seasonsItems, isLocal = true)
    }

    private suspend fun mapToSeasonItems(
      remoteSeasons: List<Season>,
      show: Show,
    ) = coroutineScope {
      val format = dateFormatProvider.loadFullHourFormat()
      val seasonsRatings = ratingsRepository.shows.loadSeasonRatings(show.ids.tmdb)
      val spoilers = settingsRepository.spoilers.getAll()
      remoteSeasons
        .map {
          val userRating =
            RatingState(
              userRating = seasonsRatings[it.number],
            )
          val episodes =
            it.episodes
              .map { episode ->
                async {
                  val rating = ratingsRepository.shows.loadRating(show.ids.tmdb, episode)
                  val translation = translationsRepository.loadTranslation(episode, show.ids.tmdb, onlyLocal = true)
                  EpisodeListItem(
                    episode = episode,
                    season = it,
                    isWatched = false,
                    translation = translation,
                    myRating = rating,
                    dateFormat = format,
                    isAnime = show.isAnime,
                    spoilers = spoilers,
                  )
                }
              }.awaitAll()
          SeasonListItem(
            show = show,
            season = it,
            episodes = episodes,
            isWatched = false,
            userRating = userRating,
            updatedAt = nowUtcMillis(),
            isRatingHidden = spoilers.isEpisodeRatingHidden,
            isRatingTapToReveal = spoilers.isTapToReveal,
          )
        }.sortedByDescending { it.season.number }
    }
  }
