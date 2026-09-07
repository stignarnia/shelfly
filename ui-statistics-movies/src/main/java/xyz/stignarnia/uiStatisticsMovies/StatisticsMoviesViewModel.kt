package xyz.stignarnia.uiStatisticsMovies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.findReplace
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiStatisticsMovies.cases.StatisticsMoviesLoadRatingsCase
import xyz.stignarnia.uiStatisticsMovies.views.ratings.recycler.StatisticsMoviesRatingItem
import javax.inject.Inject

@HiltViewModel
class StatisticsMoviesViewModel
  @Inject
  constructor(
    private val ratingsCase: StatisticsMoviesLoadRatingsCase,
    private val moviesRepository: MoviesRepository,
    private val imagesProvider: MovieImagesProvider,
  ) : ViewModel() {
    private val totalTimeSpentMinutesState = MutableStateFlow<Int?>(null)
    private val totalWatchedMoviesState = MutableStateFlow<Int?>(null)
    private val topGenresState = MutableStateFlow<List<Genre>?>(null)
    private val ratingsState = MutableStateFlow<List<StatisticsMoviesRatingItem>?>(null)

    fun loadData(initialDelay: Long = 150L) {
      viewModelScope.launch {
        val myMovies = moviesRepository.myMovies.loadAll().distinctBy { it.tmdbId }
        val genres = extractTopGenres(myMovies)

        delay(initialDelay) // Let transition finish peacefully.

        totalWatchedMoviesState.value = myMovies.count()
        totalTimeSpentMinutesState.value = myMovies.sumOf { it.runtime }
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

    fun loadMissingRatingImage(
      item: StatisticsMoviesRatingItem,
      force: Boolean,
    ) {
      viewModelScope.launch {
        updateRatingItem(item.copy(isLoading = true))
        try {
          val image = imagesProvider.loadRemoteImage(item.movie, item.image.type, force)
          updateRatingItem(item.copy(isLoading = false, image = image))
        } catch (t: Throwable) {
          updateRatingItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
        }
      }
    }

    private fun updateRatingItem(newItem: StatisticsMoviesRatingItem) {
      val items = ratingsState.value?.toMutableList() ?: return
      items.findReplace(newItem) { it.movie.ids.tmdb == newItem.movie.ids.tmdb }
      ratingsState.value = items
    }

    private fun extractTopGenres(movies: List<Movie>) =
      movies
        .flatMap { it.genres }
        .asSequence()
        .filter { it.isNotBlank() }
        .distinct()
        .map { genre -> Pair(Genre.fromSlug(genre), movies.count { genre in it.genres }) }
        .sortedByDescending { it.second }
        .map { it.first }
        .toList()
        .filterNotNull()

    val uiState =
      combine(
        totalWatchedMoviesState,
        totalTimeSpentMinutesState,
        topGenresState,
        ratingsState,
      ) { s1, s2, s3, s4 ->
        StatisticsMoviesUiState(
          totalWatchedMovies = s1,
          totalTimeSpentMinutes = s2,
          topGenres = s3,
          ratings = s4,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = StatisticsMoviesUiState(),
      )
  }
