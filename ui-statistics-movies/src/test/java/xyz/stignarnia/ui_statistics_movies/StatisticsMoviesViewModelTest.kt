package xyz.stignarnia.ui_statistics_movies

import BaseMockTest
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.UserRating
import xyz.stignarnia.ui_statistics_movies.cases.StatisticsMoviesLoadRatingsCase
import xyz.stignarnia.ui_statistics_movies.views.ratings.recycler.StatisticsMoviesRatingItem
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("EXPERIMENTAL_API_USAGE")
class StatisticsMoviesViewModelTest : BaseMockTest() {

  @MockK lateinit var ratingsCase: StatisticsMoviesLoadRatingsCase
  @MockK lateinit var moviesRepository: MoviesRepository

  private lateinit var SUT: StatisticsMoviesViewModel

  private val stateResult = mutableListOf<StatisticsMoviesUiState>()
  private val messagesResult = mutableListOf<MessageEvent>()

  @Before
  override fun setUp() {
    super.setUp()

    SUT = StatisticsMoviesViewModel(
      ratingsCase,
      moviesRepository,
    )
  }

  @After
  fun tearDown() {
    stateResult.clear()
    messagesResult.clear()
    SUT.viewModelScope.cancel()
  }

  @Test
  internal fun `Should load ratings`() =
    runTest {
      val movieItem =
        StatisticsMoviesRatingItem(Movie.EMPTY, Image.createUnknown(ImageType.POSTER), false, UserRating.EMPTY)
      coEvery { ratingsCase.loadRatings() } returns listOf(movieItem)

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }

      SUT.loadRatings()

      assertThat(stateResult.last().ratings?.size).isEqualTo(1)
      assertThat(stateResult.last().ratings).contains(movieItem)

      job.cancel()
    }

  @Test
  internal fun `Should load empty ratings in case of error`() =
    runTest {
      coEvery { ratingsCase.loadRatings() } throws Throwable("Test error")

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }

      SUT.loadRatings()

      assertThat(stateResult.last().ratings).isEmpty()

      job.cancel()
    }

  @Test
  internal fun `Should load statistics properly`() =
    runTest {
      val movies = listOf(
        Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(1)), runtime = 1, genres = listOf("war", "drama")),
        Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(2)), runtime = 2, genres = listOf("war", "animation")),
        Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(3)), runtime = 3, genres = listOf("war", "animation")),
      )

      coEvery { moviesRepository.myMovies.loadAll() } returns movies

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }

      SUT.loadData(initialDelay = 0)

      val result = stateResult.last()
      assertThat(result.totalWatchedMovies).isEqualTo(3)
      assertThat(result.totalTimeSpentMinutes).isEqualTo(6)
      assertThat(result.topGenres?.size).isEqualTo(3)
      assertThat(result.topGenres).containsExactly(Genre.WAR, Genre.ANIMATION, Genre.DRAMA)

      job.cancel()
    }
}
