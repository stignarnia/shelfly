package xyz.stignarnia.ui_search.cases

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.dao.RecentSearchDao
import xyz.stignarnia.data_local.database.model.RecentSearch
import xyz.stignarnia.ui_search.BaseMockTest
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SearchRecentsCaseTest : BaseMockTest() {

  @RelaxedMockK lateinit var database: LocalDataSource
  @RelaxedMockK lateinit var recentSearchDao: RecentSearchDao
  @RelaxedMockK lateinit var recentSearch: RecentSearch

  private lateinit var SUT: SearchRecentsCase

  @Before
  override fun setUp() {
    super.setUp()

    coEvery { recentSearch.text } returnsMany listOf("1", "2", "3")
    coEvery { database.recentSearch } returns recentSearchDao

    SUT = SearchRecentsCase(testDispatchers, database)
  }

  @After
  fun tearDown() {
    confirmVerified(recentSearchDao)
    clearAllMocks()
  }

  @Test
  fun `Should return recent searches with a limit properly`() =
    runTest {
      val limit = 3
      coEvery { recentSearchDao.getAll(any()) } returns listOf(recentSearch, recentSearch, recentSearch)

      val result = SUT.getRecentSearches(limit)
      assertThat(result).hasSize(limit)

      assertThat(result[0].text).isEqualTo("1")
      assertThat(result[1].text).isEqualTo("2")
      assertThat(result[2].text).isEqualTo("3")

      coVerify(exactly = 1) { recentSearchDao.getAll(any()) }
    }

  @Test
  fun `Should clear recent searches properly`() =
    runTest {
      SUT.clearRecentSearches()
      coVerify(exactly = 1) { recentSearchDao.deleteAll() }
    }

  @Test
  fun `Should save recent searches properly`() =
    runTest {
      val slot = slot<List<RecentSearch>>()
      coEvery { recentSearchDao.upsert(capture(slot)) } just Runs

      SUT.saveRecentSearch("test")

      coVerify(exactly = 1) { recentSearchDao.upsert(any()) }
      assertThat(slot.captured).hasSize(1)
      assertThat(slot.captured.first().text).contains("test")
    }
}
