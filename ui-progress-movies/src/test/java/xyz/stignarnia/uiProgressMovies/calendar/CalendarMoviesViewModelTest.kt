package xyz.stignarnia.uiProgressMovies.calendar

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
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
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiProgressMovies.BaseMockTest
import xyz.stignarnia.uiProgressMovies.calendar.cases.items.CalendarMoviesFutureCase
import xyz.stignarnia.uiProgressMovies.calendar.cases.items.CalendarMoviesRecentsCase
import xyz.stignarnia.uiProgressMovies.calendar.recycler.CalendarMovieListItem
import xyz.stignarnia.uiProgressMovies.main.ProgressMoviesMainUiState

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarMoviesViewModelTest : BaseMockTest() {
  @MockK lateinit var recentsCase: CalendarMoviesRecentsCase

  @MockK lateinit var futureCase: CalendarMoviesFutureCase

  @MockK lateinit var imagesProvider: MovieImagesProvider

  @MockK lateinit var translationsRepository: TranslationsRepository

  private lateinit var SUT: CalendarMoviesViewModel
  private val parentState = ProgressMoviesMainUiState(calendarMode = CalendarMode.PRESENT_FUTURE)

  private val stateResult = mutableListOf<CalendarMoviesUiState>()
  private val messagesResult = mutableListOf<MessageEvent>()

  private val movieItem =
    CalendarMovieListItem.MovieItem(
      movie = Movie.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
      isWatched = false,
      isWatchlist = false,
      translation = null,
      dateFormat = null,
      spoilers = SpoilersSettings.INITIAL,
    )

  @Before
  override fun setUp() {
    super.setUp()

    coEvery { futureCase.loadItems(any()) } returns listOf()
    coEvery { recentsCase.loadItems(any()) } returns listOf()

    SUT = CalendarMoviesViewModel(recentsCase, futureCase, imagesProvider, translationsRepository)
  }

  @After
  fun tearDown() {
    stateResult.clear()
    messagesResult.clear()
    SUT.viewModelScope.cancel()
  }

  @Test
  fun `Should load items if parent timestamp changed`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { futureCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 123))

      assertThat(stateResult.last().items).containsExactly(movieItem)
      coVerify(exactly = 1) { futureCase.loadItems(any()) }
      coVerify { recentsCase wasNot Called }
      job.cancel()
    }

  @Test
  fun `Should not reload items if parent timestamp is the same`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { futureCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 0))

      assertThat(stateResult.lastOrNull()?.items).isNull()
      coVerify { futureCase wasNot Called }
      job.cancel()
    }

  @Test
  fun `Should load items if calendar mode changed`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { recentsCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 0, calendarMode = CalendarMode.RECENTS))

      assertThat(stateResult.last().items).containsExactly(movieItem)
      coVerify(exactly = 1) { recentsCase.loadItems(any()) }
      coVerify { futureCase wasNot Called }
      job.cancel()
    }

  @Test
  fun `Should load items if search query changed`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { futureCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 0, searchQuery = "test"))

      assertThat(stateResult.last().items).containsExactly(movieItem)
      coVerify(exactly = 1) { futureCase.loadItems(any()) }
      coVerify { recentsCase wasNot Called }
      job.cancel()
    }

  @Test
  fun `Should not reload items if parent search query is the same`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { futureCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 0, searchQuery = "test"))
      SUT.onParentState(parentState.copy(timestamp = 0, searchQuery = "test"))

      assertThat(stateResult.last().items).containsExactly(movieItem)
      coVerify(exactly = 1) { futureCase.loadItems(any()) }
      coVerify { recentsCase wasNot Called }
      job.cancel()
    }
}
