package xyz.stignarnia.repository

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.database.dao.ShowsDao
import xyz.stignarnia.data_local.database.model.Show
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.repository.common.BaseMockTest
import xyz.stignarnia.repository.shows.ShowDetailsRepository
import xyz.stignarnia.ui_model.IdTmdb
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import xyz.stignarnia.data_remote.catalog.model.Show as ShowRemote

class ShowDetailsRepositoryTest : BaseMockTest() {

  @MockK lateinit var catalogApi: TmdbRemoteDataSource
  @MockK lateinit var showsDao: ShowsDao

  private lateinit var SUT: ShowDetailsRepository

  @Before
  override fun setUp() {
    super.setUp()
    every { database.shows } returns showsDao
    every { cloud.tmdb } returns catalogApi

    SUT = ShowDetailsRepository(cloud, database, transactions, mappers)
  }

  @Test
  fun `Should load cached show details on given conditions`() {
    runBlocking {
      val showDb = mockk<Show>(relaxed = true) {
        every { idTmdb } returns 1
        every { idImdb } returns "tt0000001"
        every { updatedAt } returns nowUtcMillis() - 100
      }
      coEvery { showsDao.getById(any<Long>()) } returns showDb

      val show = SUT.load(IdTmdb(1), false)

      assertThat(show.ids.tmdb).isEqualTo(IdTmdb(1))
      coVerify(exactly = 1) { showsDao.getById(any<Long>()) }
      coVerify(exactly = 0) { catalogApi.fetchShow(any<Long>()) }
    }
  }

  @Test
  fun `Should load remote show details if cached show has no IMDb id`() {
    runBlocking {
      // Only the details endpoint appends external_ids, so a row cached by a
      // list endpoint has no IMDb id and external ratings cannot be looked up.
      val showDb = mockk<Show>(relaxed = true) {
        every { idTmdb } returns 1
        every { idImdb } returns ""
        every { updatedAt } returns nowUtcMillis() - 100
      }
      val showRemote = mockk<ShowRemote>(relaxed = true) {
        every { ids?.tmdb } returns 1
        every { ids?.imdb } returns "tt0000001"
      }
      coEvery { showsDao.getById(any<Long>()) } returns showDb
      coEvery { showsDao.upsert(any()) } just Runs
      coEvery { catalogApi.fetchShow(any<Long>()) } returns showRemote

      val show = SUT.load(IdTmdb(1), false)

      assertThat(show.ids.imdb.id).isEqualTo("tt0000001")
      coVerify(exactly = 1) { catalogApi.fetchShow(any<Long>()) }
    }
  }

  @Test
  fun `Should load remote show details if force flag is set`() {
    runBlocking {
      val showRemote = mockk<ShowRemote>(relaxed = true) {
        every { ids?.tmdb } returns 1
      }
      coEvery { showsDao.getById(any<Long>()) } returns null
      coEvery { showsDao.upsert(any()) } just Runs
      coEvery { catalogApi.fetchShow(any<Long>()) } returns showRemote

      val show = SUT.load(IdTmdb(1), true)

      assertThat(show.ids.tmdb).isEqualTo(IdTmdb(1))

      coVerifySequence {
        showsDao.getById(any<Long>())
        catalogApi.fetchShow(any<Long>())
        showsDao.upsert(any())
      }
    }
  }

  @Test
  fun `Should load remote show details if nothing is cached`() {
    runBlocking {
      val showRemote = mockk<ShowRemote>(relaxed = true) {
        every { ids?.tmdb } returns 1
      }
      coEvery { showsDao.getById(any<Long>()) } returns null
      coEvery { showsDao.upsert(any()) } just Runs
      coEvery { catalogApi.fetchShow(any<Long>()) } returns showRemote

      val show = SUT.load(IdTmdb(1), false)

      assertThat(show.ids.tmdb).isEqualTo(IdTmdb(1))

      coVerifySequence {
        showsDao.getById(any<Long>())
        catalogApi.fetchShow(any<Long>())
        showsDao.upsert(any())
      }
    }
  }

  @Test
  fun `Should load remote show details if cached show expired`() {
    runBlocking {
      val showDb = mockk<Show>(relaxed = true) {
        every { idTmdb } returns 1
        every { updatedAt } returns nowUtcMillis() - TimeUnit.DAYS.toMillis(10)
      }
      val showRemote = mockk<ShowRemote>(relaxed = true) {
        every { ids?.tmdb } returns 1
      }
      coEvery { showsDao.getById(any<Long>()) } returns showDb
      coEvery { showsDao.upsert(any()) } just Runs
      coEvery { catalogApi.fetchShow(any<Long>()) } returns showRemote

      val show = SUT.load(IdTmdb(1), false)

      assertThat(show.ids.tmdb).isEqualTo(IdTmdb(1))

      coVerifySequence {
        showsDao.getById(any<Long>())
        catalogApi.fetchShow(any<Long>())
        showsDao.upsert(any())
      }
    }
  }
}
