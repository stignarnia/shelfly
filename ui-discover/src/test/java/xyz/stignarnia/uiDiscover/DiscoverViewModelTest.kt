package xyz.stignarnia.uiDiscover

import BaseMockTest
import TestData
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiDiscover.cases.DiscoverFiltersCase
import xyz.stignarnia.uiDiscover.cases.DiscoverShowsCase
import xyz.stignarnia.uiModel.DiscoverFilters

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest : BaseMockTest() {
  @MockK internal lateinit var showsCase: DiscoverShowsCase

  @MockK lateinit var filtersCase: DiscoverFiltersCase

  @MockK lateinit var imagesProvider: ShowImagesProvider

  @MockK lateinit var apiKeyProvider: ApiKeyProvider

  private lateinit var SUT: DiscoverViewModel

  @Before
  override fun setUp() {
    super.setUp()

    coEvery { filtersCase.loadFilters() } returns DiscoverFilters()
    coEvery { showsCase.loadCachedShows(any()) } returns emptyList()
    coEvery { showsCase.loadRemoteShows(any()) } returns emptyList()
    coEvery { apiKeyProvider.hasTmdbApiKey() } returns true

    SUT = DiscoverViewModel(showsCase, filtersCase, imagesProvider, apiKeyProvider)
  }

  @After
  fun tearDown() {
    SUT.viewModelScope.cancel()
  }

  @Test
  fun `Should load cached data and not load remote data if cache is valid`() {
    coEvery { showsCase.isCacheValid() } returns true

    SUT.loadShows()

    coVerify(exactly = 1) { showsCase.loadCachedShows(any()) }
    coVerify(exactly = 0) { showsCase.loadRemoteShows(any()) }
  }

  @Test
  fun `Should load cached data and load remote data if cache is no longer valid`() {
    coEvery { showsCase.isCacheValid() } returns false

    SUT.loadShows()

    coVerify(exactly = 1) { showsCase.loadCachedShows(any()) }
    coVerify(exactly = 1) { showsCase.loadRemoteShows(any()) }
  }

  @Test
  fun `Should not load remote data if no TMDB key is set`() {
    coEvery { showsCase.isCacheValid() } returns false
    coEvery { apiKeyProvider.hasTmdbApiKey() } returns false

    SUT.loadShows()

    coVerify(exactly = 1) { showsCase.loadCachedShows(any()) }
    coVerify(exactly = 0) { showsCase.loadRemoteShows(any()) }
  }

  @Test
  fun `Should not load remote data on pull to refresh if no TMDB key is set`() {
    coEvery { apiKeyProvider.hasTmdbApiKey() } returns false

    SUT.loadShows(pullToRefresh = true)

    coVerify(exactly = 0) { showsCase.loadRemoteShows(any()) }
  }

  @Test
  fun `Should load remote data only if pull to refresh`() {
    coEvery { showsCase.isCacheValid() } returns true

    SUT.loadShows(pullToRefresh = true)

    coVerify(exactly = 0) { showsCase.loadCachedShows(any()) }
    coVerify(exactly = 1) { showsCase.loadRemoteShows(any()) }
  }

  @Test
  fun `Should load remote data only if skipping cache`() {
    coEvery { showsCase.isCacheValid() } returns true

    SUT.loadShows(skipCache = true)

    coVerify(exactly = 0) { showsCase.loadCachedShows(any()) }
    coVerify(exactly = 1) { showsCase.loadRemoteShows(any()) }
  }

  @Test
  fun `Should not load cached data if skipping cache`() {
    SUT.loadShows(skipCache = true)
    coVerify(exactly = 0) { showsCase.loadCachedShows(any()) }
  }

  @Test
  fun `Should show loading state instantly if pull to refresh`() =
    runTest {
      val stateResult = mutableListOf<DiscoverUiState>()
      val messagesResult = mutableListOf<MessageEvent>()

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      val job2 = launch(UnconfinedTestDispatcher()) { SUT.messageFlow.toList(messagesResult) }

      SUT.loadShows(pullToRefresh = true)

      assertThat(stateResult[0].isLoading).isNull()
      assertThat(stateResult[1].isLoading).isFalse()
      assertThat(stateResult[2].isLoading).isTrue()
      assertThat(messagesResult).isEmpty()

      job.cancel()
      job2.cancel()
    }

  @Test
  fun `Should not emit cached results if pull to refresh`() =
    runTest {
      val stateResult = mutableListOf<DiscoverUiState>()
      val messagesResult = mutableListOf<MessageEvent>()

      val job = launch { SUT.uiState.toList(stateResult) }
      val job2 = launch { SUT.messageFlow.toList(messagesResult) }

      val cachedItem = TestData.DISCOVER_LIST_ITEM
      coEvery { showsCase.loadCachedShows(any()) } returns listOf(cachedItem)

      SUT.loadShows(pullToRefresh = true)

      stateResult.forEach {
        assertThat(it.items.isNullOrEmpty()).isTrue()
      }
      assertThat(messagesResult).isEmpty()

      job.cancel()
      job2.cancel()
    }

  @Test
  fun `Should not post cached results if skipping cache`() =
    runTest {
      val stateResult = mutableListOf<DiscoverUiState>()
      val messagesResult = mutableListOf<MessageEvent>()

      val job = launch { SUT.uiState.toList(stateResult) }
      val job2 = launch { SUT.messageFlow.toList(messagesResult) }

      val cachedItem = TestData.DISCOVER_LIST_ITEM
      coEvery { showsCase.loadCachedShows(any()) } returns listOf(cachedItem)

      SUT.loadShows(skipCache = true)

      stateResult.forEach {
        assertThat(it.items.isNullOrEmpty()).isTrue()
      }
      assertThat(messagesResult).isEmpty()

      job.cancel()
      job2.cancel()
    }

  @Test
  fun `Should post cached results and then fresh remote results`() =
    runTest {
      val stateResult = mutableListOf<DiscoverUiState>()
      val messagesResult = mutableListOf<MessageEvent>()

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      val job2 = launch(UnconfinedTestDispatcher()) { SUT.messageFlow.toList(messagesResult) }

      val cachedItem = TestData.DISCOVER_LIST_ITEM
      val remoteItem = cachedItem.copy(isFollowed = true)
      coEvery { showsCase.loadCachedShows(any()) } returns listOf(cachedItem)
      coEvery { showsCase.loadRemoteShows(any()) } coAnswers {
        delay(1000)
        listOf(remoteItem)
      }
      coEvery { showsCase.isCacheValid() } returns false

      SUT.loadShows()
      advanceUntilIdle()

      assertThat(stateResult.any { it.items?.contains(cachedItem) == true }).isTrue()
      assertThat(stateResult.last().items?.contains(remoteItem)).isTrue()
      assertThat(messagesResult).isEmpty()

      job.cancel()
      job2.cancel()
    }

  @Test
  fun `Should post error message on error`() =
    runTest {
      val stateResult = mutableListOf<DiscoverUiState>()
      val messagesResult = mutableListOf<MessageEvent>()

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      val job2 = launch(UnconfinedTestDispatcher()) { SUT.messageFlow.toList(messagesResult) }

      coEvery { showsCase.loadCachedShows(any()) } throws Error()

      SUT.loadShows()

      assertThat(messagesResult.last().consume()).isEqualTo(xyz.stignarnia.uiBase.R.string.errorCouldNotLoadDiscover)

      job.cancel()
      job2.cancel()
    }
}
