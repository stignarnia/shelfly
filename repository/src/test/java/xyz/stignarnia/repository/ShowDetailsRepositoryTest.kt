package xyz.stignarnia.repository

import com.google.common.truth.Truth.assertThat
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
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.database.dao.ShowsDao
import xyz.stignarnia.dataLocal.database.model.Show
import xyz.stignarnia.dataRemote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.repository.common.BaseMockTest
import xyz.stignarnia.repository.shows.ShowDetailsRepository
import xyz.stignarnia.uiModel.IdTmdb
import java.util.concurrent.TimeUnit
import xyz.stignarnia.dataRemote.catalog.model.Show as ShowRemote

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

  private fun createShowDb(
    idTmdb: Long = 1,
    idImdb: String = "tt0000001",
    updatedAt: Long = nowUtcMillis() - 100,
  ) = Show(
    idTmdb = idTmdb,
    idTvdb = 1,
    idImdb = idImdb,
    idSlug = "show-1",
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
    updatedAt = updatedAt,
  )

  private fun createShowRemote(
    tmdbId: Long = 1,
    imdbId: String = "tt0000001",
  ) = ShowRemote(
    ids =
      xyz.stignarnia.dataRemote.catalog.model.Ids(
        slug = null,
        tvdb = null,
        imdb = imdbId,
        tmdb = tmdbId,
        tvrage = null,
      ),
    title = "Show",
    year = 2020,
    overview = null,
    first_aired = null,
    runtime = null,
    airs = null,
    certification = null,
    network = null,
    country = null,
    trailer = null,
    homepage = null,
    status = null,
    rating = null,
    votes = null,
    comment_count = null,
    genres = null,
    aired_episodes = null,
  )

  @Test
  fun `Should load cached show details on given conditions`() {
    runBlocking {
      val showDb = createShowDb()
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
      val showDb = createShowDb(idImdb = "")
      val showRemote = createShowRemote(tmdbId = 1, imdbId = "tt0000001")
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
      val showRemote = createShowRemote(tmdbId = 1)
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
      val showRemote = createShowRemote(tmdbId = 1)
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
      val showDb = createShowDb(updatedAt = nowUtcMillis() - TimeUnit.DAYS.toMillis(10))
      val showRemote = createShowRemote(tmdbId = 1)
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
