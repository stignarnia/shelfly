package xyz.stignarnia.ui_statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.Config
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Episode
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.combine
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType.POSTER
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_statistics.cases.StatisticsLoadRatingsCase
import xyz.stignarnia.ui_statistics.views.mostWatched.StatisticsMostWatchedItem
import xyz.stignarnia.ui_statistics.views.ratings.recycler.StatisticsRatingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
  private val ratingsCase: StatisticsLoadRatingsCase,
  private val showsRepository: ShowsRepository,
  private val translationsRepository: TranslationsRepository,
  private val imagesProvider: ShowImagesProvider,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) : ViewModel() {

  private val mostWatchedShowsState = MutableStateFlow<List<StatisticsMostWatchedItem>?>(null)
  private val mostWatchedTotalCountState = MutableStateFlow<Int?>(null)
  private val totalTimeSpentMinutesState = MutableStateFlow<Int?>(null)
  private val totalWatchedEpisodesState = MutableStateFlow<Int?>(null)
  private val totalWatchedEpisodesShowsState = MutableStateFlow<Int?>(null)
  private val topGenresState = MutableStateFlow<List<Genre>?>(null)
  private val ratingsState = MutableStateFlow<List<StatisticsRatingItem>?>(null)

  private var takeLimit = 5

  fun loadData(
    limit: Int = 0,
    initialDelay: Long = 150L,
  ) {
    takeLimit += limit
    viewModelScope.launch {
      val language = translationsRepository.getLanguage()

      val myShows = showsRepository.myShows.loadAll()
      val watchlistShows = showsRepository.watchlistShows.loadAll() // Add shows from watchlist
      val hiddenShows = showsRepository.hiddenShows.loadAll()

      val shows = (myShows + watchlistShows + hiddenShows).distinctBy { it.tmdbId }
      val showsIds = shows.map { it.tmdbId }

      val episodes = batchEpisodes(showsIds)
      val seasons = batchSeasons(showsIds)

      val genres = extractTopGenres(shows)
      val mostWatchedShows = shows
        .map { show ->
          val translation = loadTranslation(language, show)
          StatisticsMostWatchedItem(
            show = shows.first { it.tmdbId == show.tmdbId },
            seasonsCount = seasons.filter { it.idShowTmdb == show.tmdbId }.count().toLong(),
            episodes = episodes
              .filter { it.idShowTmdb == show.tmdbId }
              .map { mappers.episode.fromDatabase(it) },
            image = Image.createUnknown(POSTER),
            translation = translation,
          )
        }.sortedByDescending { item -> item.episodes.sumOf { it.runtime } }
        .take(takeLimit)
        .map {
          it.copy(image = imagesProvider.findCachedImage(it.show, POSTER))
        }

      delay(initialDelay) // Let transition finish peacefully.

      mostWatchedShowsState.value = mostWatchedShows
      mostWatchedTotalCountState.value = showsIds.size
      totalTimeSpentMinutesState.value = episodes.sumOf { it.runtime }
      totalWatchedEpisodesState.value = episodes.count()
      totalWatchedEpisodesShowsState.value = episodes.distinctBy { it.idShowTmdb }.count()
      topGenresState.value = genres
    }
  }

  fun loadRatings() {
    viewModelScope.launch {
      try {
        ratingsState.value = ratingsCase.loadRatings()
      } catch (t: Throwable) {
        ratingsState.value = emptyList()
      }
    }
  }

  private suspend fun batchEpisodes(
    showsIds: List<Long>,
    allEpisodes: MutableList<Episode> = mutableListOf(),
  ): List<Episode> {
    viewModelScope.ensureActive()

    val batch = showsIds.take(500)
    if (batch.isEmpty()) return allEpisodes

    val episodes = localSource.episodes.getAllWatchedForShows(batch)
    allEpisodes.addAll(episodes)

    return batchEpisodes(showsIds.filter { it !in batch }, allEpisodes)
  }

  private suspend fun batchSeasons(
    showsIds: List<Long>,
    allSeasons: MutableList<Season> = mutableListOf(),
  ): List<Season> {
    viewModelScope.ensureActive()

    val batch = showsIds.take(500)
    if (batch.isEmpty()) return allSeasons

    val seasons = localSource.seasons.getAllWatchedForShows(batch)
    allSeasons.addAll(seasons)

    return batchSeasons(showsIds.filter { it !in batch }, allSeasons)
  }

  private fun extractTopGenres(shows: List<Show>) =
    shows
      .flatMap { it.genres }
      .asSequence()
      .filter { it.isNotBlank() }
      .distinct()
      .map { genre -> Pair(Genre.fromSlug(genre), shows.count { genre in it.genres }) }
      .sortedByDescending { it.second }
      .map { it.first }
      .toList()
      .filterNotNull()

  private suspend fun loadTranslation(
    language: String,
    show: Show,
  ) = if (language == Config.DEFAULT_LANGUAGE) {
    null
  } else {
    translationsRepository.loadTranslation(show, language, true)
  }

  val uiState = combine(
    mostWatchedShowsState,
    mostWatchedTotalCountState,
    totalTimeSpentMinutesState,
    totalWatchedEpisodesState,
    totalWatchedEpisodesShowsState,
    topGenresState,
    ratingsState,
  ) { s1, s2, s3, s4, s5, s6, s7 ->
    StatisticsUiState(
      mostWatchedShows = s1,
      mostWatchedTotalCount = s2,
      totalTimeSpentMinutes = s3,
      totalWatchedEpisodes = s4,
      totalWatchedEpisodesShows = s5,
      topGenres = s6,
      ratings = s7,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = StatisticsUiState(),
  )
}
