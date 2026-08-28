package xyz.stignarnia.repository

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.dataLocal.database.model.MyShow
import xyz.stignarnia.dataLocal.sources.ArchiveShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.MyShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.WatchlistShowsLocalDataSource
import xyz.stignarnia.repository.common.BaseMockTest
import xyz.stignarnia.repository.shows.MyShowsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.dataLocal.database.model.Show as ShowDb

class MyShowsRepositoryTest : BaseMockTest() {
  @RelaxedMockK lateinit var myShowsLocalSource: MyShowsLocalDataSource

  @RelaxedMockK lateinit var watchlistShowsLocalSource: WatchlistShowsLocalDataSource

  @RelaxedMockK lateinit var hiddenShowsLocalDataSource: ArchiveShowsLocalDataSource
  private val showDb =
    ShowDb(
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

  private lateinit var SUT: MyShowsRepository

  @Before
  override fun setUp() {
    super.setUp()
    SUT =
      MyShowsRepository(
        myShowsLocalSource,
        watchlistShowsLocalSource,
        hiddenShowsLocalDataSource,
        transactions,
        mappers,
      )
    coEvery { database.myShows } returns myShowsLocalSource
    coEvery { database.watchlistShows } returns watchlistShowsLocalSource
    coEvery { database.archiveShows } returns hiddenShowsLocalDataSource
  }

  @After
  fun confirmSutVerified() {
    confirmVerified(myShowsLocalSource)
    confirmVerified(watchlistShowsLocalSource)
    confirmVerified(hiddenShowsLocalDataSource)
  }

  @Test
  fun `Should load and map single show by TMDB ID`() {
    runBlocking {
      coEvery { myShowsLocalSource.getById(any()) } returns showDb

      val testShow = SUT.load(IdTmdb(1L))

      assertThat(testShow?.title).isEqualTo("Show")
      coVerify(exactly = 1) { myShowsLocalSource.getById(any()) }
    }
  }

  @Test
  fun `Should load and map all shows`() {
    runBlocking {
      coEvery { myShowsLocalSource.getAll() } returns listOf(showDb)

      val shows = SUT.loadAll()

      assertThat(shows).hasSize(1)
      coVerify(exactly = 1) { myShowsLocalSource.getAll() }
    }
  }

  @Test
  fun `Should load all shows ids`() {
    runBlocking {
      coEvery { myShowsLocalSource.getAllTmdbIds() } returns listOf(1L, 2L)

      val ids = SUT.loadAllIds()

      assertThat(ids).containsExactly(1L, 2L)
      coVerify(exactly = 1) { myShowsLocalSource.getAllTmdbIds() }
    }
  }

  @Test
  fun `Should load and map all shows by TMDB Ids`() {
    runBlocking {
      coEvery { myShowsLocalSource.getAll(any()) } returns listOf(showDb, showDb)

      val shows = SUT.loadAll(listOf(IdTmdb(1), IdTmdb(2)))

      assertThat(shows).hasSize(2)
      coVerify(exactly = 1) { myShowsLocalSource.getAll(listOf(1, 2)) }
    }
  }

  @Test
  fun `Should load and map all recents shows using amount`() {
    runBlocking {
      coEvery { myShowsLocalSource.getAllRecent(any()) } returns listOf(showDb, showDb)

      val shows = SUT.loadAllRecent(2)

      assertThat(shows).hasSize(2)
      coVerify(exactly = 1) { myShowsLocalSource.getAllRecent(2) }
    }
  }

  @Test
  fun `Should insert show into database using TMDB ID`() {
    runBlocking {
      val slot = slot<List<MyShow>>()
      coJustRun { myShowsLocalSource.insert(capture(slot)) }

      SUT.insert(IdTmdb(10L), 666)

      slot.captured[0].run {
        assertThat(id).isEqualTo(0)
        assertThat(idTmdb).isEqualTo(10)
        assertThat(createdAt).isGreaterThan(0L)
        assertThat(updatedAt).isGreaterThan(0L)
        assertThat(lastWatchedAt).isEqualTo(666)
      }
      coVerify(exactly = 1) { myShowsLocalSource.insert(any()) }
      coVerify(exactly = 1) { watchlistShowsLocalSource.deleteById(any()) }
      coVerify(exactly = 1) { hiddenShowsLocalDataSource.deleteById(any()) }
    }
  }

  @Test
  fun `Should delete show from database using TMDB ID`() {
    runBlocking {
      val slot = slot<Long>()
      coJustRun { myShowsLocalSource.deleteById(capture(slot)) }

      SUT.delete(IdTmdb(10L))

      assertThat(slot.captured).isEqualTo(10L)
      coVerify(exactly = 1) { myShowsLocalSource.deleteById(10L) }
    }
  }
}
