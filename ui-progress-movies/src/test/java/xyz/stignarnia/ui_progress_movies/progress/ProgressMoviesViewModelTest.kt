package xyz.stignarnia.ui_progress_movies.progress

import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_progress_movies.BaseMockTest
import xyz.stignarnia.ui_progress_movies.main.ProgressMoviesMainUiState
import xyz.stignarnia.ui_progress_movies.progress.cases.ProgressMoviesItemsCase
import xyz.stignarnia.ui_progress_movies.progress.cases.ProgressMoviesPinnedCase
import xyz.stignarnia.ui_progress_movies.progress.cases.ProgressMoviesSortCase
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMovieListItem
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressMoviesViewModelTest : BaseMockTest() {

  @MockK lateinit var itemsCase: ProgressMoviesItemsCase
  @MockK lateinit var sortCase: ProgressMoviesSortCase
  @MockK lateinit var pinnedCase: ProgressMoviesPinnedCase
  @MockK lateinit var imagesProvider: MovieImagesProvider
  @MockK lateinit var workManager: WorkManager
  private lateinit var settingsRepository: SettingsRepository
  @MockK lateinit var translationsRepository: TranslationsRepository

  private lateinit var SUT: ProgressMoviesViewModel
  private val parentState = ProgressMoviesMainUiState()

  private val stateResult = mutableListOf<ProgressMoviesUiState>()
  private val messagesResult = mutableListOf<MessageEvent>()

  @Before
  override fun setUp() {
    super.setUp()

    coEvery { translationsRepository.getLanguage() } returns "en"

    // The constructor starts watching the manual backup run.
    // Nothing here is about that indicator, so the work stream stays empty - without a stub the mock throws on the collect and takes down every test in the class.
    every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns emptyFlow()

    settingsRepository = SettingsRepository(
      sorting = mockk(),
      filters = mockk(),
      widgets = mockk(),
      viewMode = mockk(),
      spoilers = mockk(),
      sync = mockk(),
      webdav = mockk(),
      dispatchers = testDispatchers,
      localSource = mockk(),
      transactions = mockk(),
      mappers = mockk(),
      preferences = mockk(),
    )

    SUT = ProgressMoviesViewModel(
      itemsCase,
      sortCase,
      pinnedCase,
      imagesProvider,
      workManager,
      settingsRepository,
      translationsRepository,
    )
  }

  private val movieItem = ProgressMovieListItem.MovieItem(
    movie = Movie.EMPTY,
    image = Image.createUnknown(ImageType.POSTER),
    isLoading = false,
    isPinned = false,
    translation = null,
    dateFormat = null,
    sortOrder = null,
    userRating = null,
    spoilers = SpoilersSettings.INITIAL,
  )

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
      coEvery { itemsCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 123))

      assertThat(stateResult.last().items).containsExactly(movieItem)
      coVerify(exactly = 1) { itemsCase.loadItems(any()) }
      job.cancel()
    }

  @Test
  fun `Should not reload items if parent timestamp is the same`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { itemsCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 0))

      assertThat(stateResult.lastOrNull()?.items).isNull()
      coVerify { itemsCase wasNot Called }
      job.cancel()
    }

  @Test
  fun `Should load items if search query changed`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { itemsCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 0, searchQuery = "test"))

      assertThat(stateResult.last().items).containsExactly(movieItem)
      coVerify(exactly = 1) { itemsCase.loadItems(any()) }
      job.cancel()
    }

  @Test
  fun `Should not reload items if parent search query is the same`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { itemsCase.loadItems(any()) } returns listOf(movieItem)

      SUT.onParentState(parentState.copy(timestamp = 0, searchQuery = "test"))
      SUT.onParentState(parentState.copy(timestamp = 0, searchQuery = "test"))

      assertThat(stateResult.last().items).containsExactly(movieItem)
      coVerify(exactly = 1) { itemsCase.loadItems(any()) }
      job.cancel()
    }

  @Test
  fun `Should toggle pinned item if pinned`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { pinnedCase.addPinnedItem(any()) } just Runs
      coEvery { pinnedCase.removePinnedItem(any()) } just Runs
      coEvery { itemsCase.loadItems(any()) } returns emptyList()

      val item = ProgressMovieListItem.MovieItem(
        movie = Movie.EMPTY,
        image = Image.createUnknown(ImageType.POSTER),
        isLoading = false,
        isPinned = true,
        translation = null,
        dateFormat = null,
        sortOrder = null,
        userRating = null,
        spoilers = SpoilersSettings.INITIAL,
      )

      SUT.togglePinItem(item)

      coVerify(exactly = 1) { pinnedCase.removePinnedItem(any()) }
      coVerify(exactly = 0) { pinnedCase.addPinnedItem(any()) }
      coVerify(exactly = 1) { itemsCase.loadItems(any()) }
      job.cancel()
    }

  @Test
  fun `Should toggle pinned item if not pinned`() =
    runTest {
      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      coEvery { pinnedCase.addPinnedItem(any()) } just Runs
      coEvery { pinnedCase.removePinnedItem(any()) } just Runs
      coEvery { itemsCase.loadItems(any()) } returns emptyList()

      val item = ProgressMovieListItem.MovieItem(
        movie = Movie.EMPTY,
        image = Image.createUnknown(ImageType.POSTER),
        isLoading = false,
        isPinned = false,
        translation = null,
        dateFormat = null,
        sortOrder = null,
        userRating = null,
        spoilers = SpoilersSettings.INITIAL,
      )

      SUT.togglePinItem(item)

      coVerify(exactly = 0) { pinnedCase.removePinnedItem(any()) }
      coVerify(exactly = 1) { pinnedCase.addPinnedItem(any()) }
      coVerify(exactly = 1) { itemsCase.loadItems(any()) }
      job.cancel()
    }
}
