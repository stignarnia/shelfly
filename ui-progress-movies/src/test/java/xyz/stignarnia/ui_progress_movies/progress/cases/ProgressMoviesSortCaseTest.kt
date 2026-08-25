package xyz.stignarnia.ui_progress_movies.progress.cases

import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.settings.SettingsSortRepository
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_progress_movies.BaseMockTest
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProgressMoviesSortCaseTest : BaseMockTest() {

  @RelaxedMockK lateinit var sortRepository: SettingsSortRepository
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var SUT: ProgressMoviesSortCase

  @Before
  override fun setUp() {
    super.setUp()
    settingsRepository = SettingsRepository(
      sorting = sortRepository,
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
    SUT = ProgressMoviesSortCase(settingsRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `Should set sorting order properly`() =
    runTest {
      SUT.setSortOrder(SortOrder.RANK, SortType.DESCENDING)

      verify { sortRepository.progressMoviesSortOrder = SortOrder.RANK }
      verify { sortRepository.progressMoviesSortType = SortType.DESCENDING }
    }
}
