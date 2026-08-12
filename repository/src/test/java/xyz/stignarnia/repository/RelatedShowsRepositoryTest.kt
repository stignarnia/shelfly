package xyz.stignarnia.repository

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.database.dao.RelatedShowsDao
import xyz.stignarnia.data_local.database.dao.ShowsDao
import xyz.stignarnia.data_local.database.model.RelatedShow
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.repository.common.BaseMockTest
import xyz.stignarnia.repository.shows.RelatedShowsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit.HOURS

class RelatedShowsRepositoryTest : BaseMockTest() {

  @MockK
  lateinit var catalogApi: TmdbRemoteDataSource

  @RelaxedMockK
  lateinit var relatedShowsDao: RelatedShowsDao

  @MockK
  lateinit var showsDao: ShowsDao

  private lateinit var SUT: RelatedShowsRepository

  @Before
  override fun setUp() {
    super.setUp()
    every { database.shows } returns showsDao
    every { database.relatedShows } returns relatedShowsDao
    every { cloud.tmdb } returns catalogApi

    SUT = RelatedShowsRepository(cloud, database, transactions, mappers)
  }

  @Test
  fun `Should return cached shows properly`() {
    runBlocking {
      val showDb = mockk<RelatedShow>(relaxed = true) {
        every { updatedAt } returns nowUtcMillis() - HOURS.toMillis(1)
      }
      coEvery { showsDao.getAll(any()) } returns emptyList()
      coEvery { relatedShowsDao.getAllById(any()) } returns listOf(showDb)

      SUT.loadAll(mockk(relaxed = true), 0)

      coVerifySequence {
        relatedShowsDao.getAllById(any())
        showsDao.getAll(any())
      }
      coVerify(exactly = 0) { catalogApi.fetchRelatedShows(any()) }
    }
  }

  @Test
  fun `Should return remote shows if nothing is cached`() {
    runBlocking {
      coEvery { showsDao.getAll(any()) } returns emptyList()
      coEvery { showsDao.upsert(any()) } just Runs
      coEvery { catalogApi.fetchRelatedShows(any()) } returns listOf(mockk(relaxed = true))
      coEvery { relatedShowsDao.getAllById(any()) } returns listOf()

      SUT.loadAll(mockk(relaxed = true), 0)

      coVerifyOrder {
        relatedShowsDao.getAllById(any())
        catalogApi.fetchRelatedShows(any())
      }
      coVerify(exactly = 0) { showsDao.getAll(any()) }
    }
  }

  @Test
  fun `Should return remote shows if cached values expired`() {
    runBlocking {
      val showDb = mockk<RelatedShow>(relaxed = true) {
        every { updatedAt } returns nowUtcMillis() - (Config.RELATED_CACHE_DURATION + 1000)
      }
      coEvery { showsDao.getAll(any()) } returns emptyList()
      coEvery { showsDao.upsert(any()) } just Runs
      coEvery { catalogApi.fetchRelatedShows(any()) } returns listOf(mockk(relaxed = true))
      coEvery { relatedShowsDao.getAllById(any()) } returns listOf(showDb)

      SUT.loadAll(mockk(relaxed = true), 0)

      coVerifyOrder {
        relatedShowsDao.getAllById(any())
        catalogApi.fetchRelatedShows(any())
      }
      coVerify(exactly = 0) { showsDao.getAll(any()) }
    }
  }
}
