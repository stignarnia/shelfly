package xyz.stignarnia.repository

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.data_local.database.dao.ArchiveShowsDao
import xyz.stignarnia.data_local.database.dao.MyShowsDao
import xyz.stignarnia.data_local.database.dao.WatchlistShowsDao
import xyz.stignarnia.data_local.database.model.WatchlistShow
import xyz.stignarnia.repository.common.BaseMockTest
import xyz.stignarnia.repository.shows.WatchlistShowsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Show
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.data_local.database.model.Show as ShowDb

class WatchlistShowsRepositoryTest : BaseMockTest() {

  @MockK lateinit var seeLaterShowsDao: WatchlistShowsDao
  @MockK lateinit var myShowsDao: MyShowsDao
  @MockK lateinit var archivedShowsDao: ArchiveShowsDao

  private val showDb = ShowDb(
    idTmdb = 1,
    idTvdb = 1,
    idImdb = "1",
    idSlug = "1",
    idTvrage = 1,
    title = "Show",
    year = 2020,
    overview = "",
    firstAired = "",
    runtime = 45,
    airtimeDay = "",
    airtimeTime = "",
    airtimeTimezone = "",
    certification = "",
    network = "",
    networkLogoPath = "",
    country = "",
    trailer = "",
    homepage = "",
    status = "",
    rating = 5f,
    votes = 10,
    commentCount = 0,
    genres = "",
    airedEpisodes = 10,
    createdAt = 0,
    updatedAt = 0,
  )

  private lateinit var SUT: WatchlistShowsRepository

  @Before
  override fun setUp() {
    super.setUp()
    SUT = WatchlistShowsRepository(database, transactions, mappers)

    coEvery { database.watchlistShows } returns seeLaterShowsDao
    coEvery { database.myShows } returns myShowsDao
    coEvery { database.archiveShows } returns archivedShowsDao
  }

  @After
  fun confirmSutVerified() {
    confirmVerified(seeLaterShowsDao)
  }

  @Test
  fun `Should load and map all SeeLater shows`() {
    runBlocking {
      coEvery { seeLaterShowsDao.getAll() } returns listOf(showDb)

      val shows = SUT.loadAll()

      assertThat(shows).hasSize(1)
      coVerify(exactly = 1) { seeLaterShowsDao.getAll() }
    }
  }

  @Test
  fun `Should load and map single SeeLater show by TMDB ID`() {
    runBlocking {
      coEvery { seeLaterShowsDao.getById(any()) } returns showDb

      val testShow = SUT.load(IdTmdb(1L))

      assertThat(testShow?.title).isEqualTo("Show")
      coVerify(exactly = 1) { seeLaterShowsDao.getById(any()) }
    }
  }

  @Test
  fun `Should insert show into database using TMDB ID`() {
    runBlocking {
      coJustRun { myShowsDao.deleteById(any()) }
      coJustRun { archivedShowsDao.deleteById(any()) }

      val slot = slot<WatchlistShow>()
      coJustRun { seeLaterShowsDao.insert(capture(slot)) }

      SUT.insert(IdTmdb(1L))

      assertThat(slot.captured.id).isEqualTo(0)
      assertThat(slot.captured.idTmdb).isEqualTo(1)

      coVerify(exactly = 1) { seeLaterShowsDao.insert(any()) }
    }
  }

  @Test
  fun `Should delete show from archived and my shows when inserting into see later`() {
    runBlocking {
      coJustRun { myShowsDao.deleteById(any()) }
      coJustRun { archivedShowsDao.deleteById(any()) }

      val slot = slot<WatchlistShow>()
      coJustRun { seeLaterShowsDao.insert(capture(slot)) }

      SUT.insert(IdTmdb(1L))

      assertThat(slot.captured.id).isEqualTo(0)
      assertThat(slot.captured.idTmdb).isEqualTo(1)

      coVerify(exactly = 1) { seeLaterShowsDao.insert(any()) }
      coVerify(exactly = 1) { myShowsDao.deleteById(1L) }
      coVerify(exactly = 1) { archivedShowsDao.deleteById(1L) }
    }
  }

  @Test
  fun `Should delete show from database using TMDB ID`() {
    runBlocking {
      val slot = slot<Long>()
      coJustRun { seeLaterShowsDao.deleteById(capture(slot)) }

      SUT.delete(IdTmdb(10L))

      assertThat(slot.captured).isEqualTo(10L)
      coVerify(exactly = 1) { seeLaterShowsDao.deleteById(10L) }
    }
  }

  @Test
  fun `Should load all SeeLater shows ids`() {
    runBlocking {
      coEvery { seeLaterShowsDao.getAllTmdbIds() } returns listOf(1L, 2L)

      val ids = SUT.loadAllIds()

      assertThat(ids).containsExactly(1L, 2L)
      coVerify(exactly = 1) { seeLaterShowsDao.getAllTmdbIds() }
    }
  }
}
