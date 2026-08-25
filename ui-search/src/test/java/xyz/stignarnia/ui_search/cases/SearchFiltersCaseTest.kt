package xyz.stignarnia.ui_search.cases

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.common.Mode
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_search.BaseMockTest
import xyz.stignarnia.ui_search.recycler.SearchListItem
import xyz.stignarnia.ui_search.utilities.SearchOptions
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SearchFiltersCaseTest : BaseMockTest() {

  @RelaxedMockK lateinit var preferences: SharedPreferences
  private lateinit var settingsRepository: SettingsRepository
  @RelaxedMockK lateinit var item: SearchListItem

  private lateinit var SUT: SearchFiltersCase

  @Before
  override fun setUp() {
    super.setUp()
    every { preferences.getBoolean("KEY_MOVIES_ENABLED", true) } returns true
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
      preferences = preferences,
    )
    SUT = SearchFiltersCase(settingsRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `Should pass shows and movies if filters are empty`() =
    runTest {
      val options = SearchOptions()
      val result = SUT.filter(options, item)
      assertThat(result).isTrue()
    }

  @Test
  fun `Should pass shows and movies if filters contain shows and movies`() =
    runTest {
      val options = SearchOptions(filters = listOf(Mode.SHOWS, Mode.MOVIES))
      val result = SUT.filter(options, item)
      assertThat(result).isTrue()
    }

  @Test
  fun `Should not pass shows if filters contain only movie`() =
    runTest {
      val options = SearchOptions(filters = listOf(Mode.MOVIES))
      coEvery { item.isShow } returns true
      coEvery { item.isMovie } returns false

      val result = SUT.filter(options, item)

      verify(exactly = 1) { preferences.getBoolean("KEY_MOVIES_ENABLED", true) }
      assertThat(result).isFalse()
    }

  @Test
  fun `Should not pass movies if filters contain only show`() =
    runTest {
      val options = SearchOptions(filters = listOf(Mode.SHOWS))
      coEvery { item.isShow } returns false
      coEvery { item.isMovie } returns true

      val result = SUT.filter(options, item)

      verify(exactly = 0) { preferences.getBoolean("KEY_MOVIES_ENABLED", true) }
      assertThat(result).isFalse()
    }

  @Test
  fun `Should not pass movies if movies are disabled`() =
    runTest {
      val options = SearchOptions(filters = listOf(Mode.MOVIES))
      every { preferences.getBoolean("KEY_MOVIES_ENABLED", true) } returns false

      coEvery { item.isShow } returns false
      coEvery { item.isMovie } returns true

      val result = SUT.filter(options, item)

      verify(exactly = 1) { preferences.getBoolean("KEY_MOVIES_ENABLED", true) }
      assertThat(result).isFalse()
    }
}
