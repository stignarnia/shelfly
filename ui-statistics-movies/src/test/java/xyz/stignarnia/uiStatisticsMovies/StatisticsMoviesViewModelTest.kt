package xyz.stignarnia.uiStatisticsMovies

import BaseMockTest
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.movies.MyMoviesRepository
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageFamily
import xyz.stignarnia.uiModel.ImageSource
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiStatisticsMovies.cases.StatisticsMoviesLoadRatingsCase
import xyz.stignarnia.uiStatisticsMovies.views.ratings.recycler.StatisticsMoviesRatingItem

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsMoviesViewModelTest : BaseMockTest() {
  @MockK lateinit var ratingsCase: StatisticsMoviesLoadRatingsCase

  @MockK lateinit var myMovies: MyMoviesRepository

  @MockK lateinit var imagesProvider: MovieImagesProvider

  private lateinit var moviesRepository: MoviesRepository
  private lateinit var SUT: StatisticsMoviesViewModel

  private val stateResult = mutableListOf<StatisticsMoviesUiState>()
  private val messagesResult = mutableListOf<MessageEvent>()

  @Before
  override fun setUp() {
    super.setUp()

    moviesRepository =
      MoviesRepository(
        discoverMovies = mockk(),
        relatedMovies = mockk(),
        movieDetails = mockk(),
        myMovies = myMovies,
        watchlistMovies = mockk(),
        hiddenMovies = mockk(),
      )
    SUT =
      StatisticsMoviesViewModel(
        ratingsCase,
        moviesRepository,
        imagesProvider,
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
      val movies =
        listOf(
          Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(1)), runtime = 1, genres = listOf("war", "drama")),
          Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(2)), runtime = 2, genres = listOf("war", "animation")),
          Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(3)), runtime = 3, genres = listOf("war", "animation")),
        )

      coEvery { myMovies.loadAll() } returns movies

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }

      SUT.loadData(initialDelay = 0)

      val result = stateResult.last()
      assertThat(result.totalWatchedMovies).isEqualTo(3)
      assertThat(result.totalTimeSpentMinutes).isEqualTo(6)
      assertThat(result.topGenres?.size).isEqualTo(3)
      assertThat(result.topGenres).containsExactly(Genre.WAR, Genre.ANIMATION, Genre.DRAMA)

      job.cancel()
    }

  @Test
  internal fun `Should load missing image for rating item`() =
    runTest {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(1)))
      val ratingItem = StatisticsMoviesRatingItem(movie, Image.createUnknown(ImageType.POSTER), false, UserRating.EMPTY)
      val loadedImage =
        Image.createAvailable(
          Ids.EMPTY,
          ImageType.POSTER,
          ImageFamily.MOVIE,
          "test_path",
          ImageSource.TMDB,
        )
      coEvery { ratingsCase.loadRatings() } returns listOf(ratingItem)
      coEvery { imagesProvider.loadRemoteImage(movie, ImageType.POSTER, false) } returns loadedImage

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      SUT.loadRatings()

      SUT.loadMissingRatingImage(ratingItem, false)

      val updatedItem = stateResult.last().ratings?.first { it.movie.ids.tmdb == movie.ids.tmdb }
      assertThat(updatedItem?.image).isEqualTo(loadedImage)

      job.cancel()
    }
}
