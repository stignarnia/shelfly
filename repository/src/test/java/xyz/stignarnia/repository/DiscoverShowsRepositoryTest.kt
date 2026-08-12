package xyz.stignarnia.repository

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.database.dao.DiscoverShowsDao
import xyz.stignarnia.data_local.database.dao.ShowsDao
import xyz.stignarnia.data_local.database.model.DiscoverShow
import xyz.stignarnia.repository.common.BaseMockTest
import xyz.stignarnia.repository.shows.DiscoverShowsRepository
import xyz.stignarnia.ui_model.Show
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import xyz.stignarnia.data_local.database.model.Show as ShowDb

class DiscoverShowsRepositoryTest : BaseMockTest() {

  @MockK lateinit var showsDao: ShowsDao
  @MockK lateinit var discoverShowsDao: DiscoverShowsDao

  private lateinit var SUT: DiscoverShowsRepository

  @Before
  override fun setUp() {
    super.setUp()
    SUT = DiscoverShowsRepository(cloud, database, transactions, mappers)
    coEvery { database.shows } returns showsDao
    coEvery { database.discoverShows } returns discoverShowsDao
  }

  @After
  fun confirmSutVerified() {
    confirmVerified(showsDao)
    confirmVerified(discoverShowsDao)
  }

  @Test
  fun `Should return true if cache is valid`() {
    runBlocking {
      val discoverShow = mockk<DiscoverShow> {
        every { createdAt } returns nowUtcMillis() - TimeUnit.HOURS.toMillis(6)
      }
      coEvery { discoverShowsDao.getMostRecent() } returns discoverShow

      assertThat(SUT.isCacheValid()).isTrue()
      coVerify(exactly = 1) { discoverShowsDao.getMostRecent() }
    }
  }

  @Test
  fun `Should return false if cache is not valid`() {
    runBlocking {
      val discoverShow = mockk<DiscoverShow> {
        every { createdAt } returns nowUtcMillis() - TimeUnit.HOURS.toMillis(13)
      }
      coEvery { discoverShowsDao.getMostRecent() } returns discoverShow

      assertThat(SUT.isCacheValid()).isFalse()
      coVerify(exactly = 1) { discoverShowsDao.getMostRecent() }
    }
  }

  @Test
  fun `Should load cached shows`() {
    runBlocking {
      val discoverShow = mockk<DiscoverShow> {
        every { idTmdb } returns 10
      }
      val showDb = mockk<ShowDb> {
        every { idTmdb } returns 10
      }

      coEvery { discoverShowsDao.getAll() } returns listOf(discoverShow)
      coEvery { showsDao.getAll(any()) } returns listOf(showDb)
      coEvery { mappers.show.fromDatabase(any()) } returns Show.EMPTY

      val shows = SUT.loadAllCached()

      assertThat(shows).hasSize(1)
      coVerify(exactly = 1) { discoverShowsDao.getAll() }
      coVerify(exactly = 1) { showsDao.getAll(any()) }
    }
  }
}
