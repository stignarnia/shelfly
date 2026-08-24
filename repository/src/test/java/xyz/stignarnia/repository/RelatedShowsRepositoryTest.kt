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

  private fun createShowRemote(tmdbId: Long = 2) =
    xyz.stignarnia.data_remote.catalog.model.Show(
      ids = xyz.stignarnia.data_remote.catalog.model.Ids(
        slug = null,
        tvdb = null,
        imdb = "tt1",
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
  fun `Should return cached shows properly`() {
    runBlocking {
      val showDb =
        RelatedShow(id = 0, idTmdb = 1, idTmdbRelatedShow = 2, updatedAt = nowUtcMillis() - HOURS.toMillis(1))
      coEvery { showsDao.getAll(any()) } returns emptyList()
      coEvery { relatedShowsDao.getAllById(any()) } returns listOf(showDb)

      val showInput = xyz.stignarnia.ui_model.Show.EMPTY.copy(
        ids = xyz.stignarnia.ui_model.Ids.EMPTY
          .copy(tmdb = xyz.stignarnia.ui_model.IdTmdb(1)),
      )
      SUT.loadAll(showInput, 0)

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
      coEvery { catalogApi.fetchRelatedShows(any()) } returns listOf(createShowRemote(2))
      coEvery { relatedShowsDao.getAllById(any()) } returns listOf()

      val showInput = xyz.stignarnia.ui_model.Show.EMPTY.copy(
        ids = xyz.stignarnia.ui_model.Ids.EMPTY
          .copy(tmdb = xyz.stignarnia.ui_model.IdTmdb(1)),
      )
      SUT.loadAll(showInput, 0)

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
      val showDb = RelatedShow(
        id = 0,
        idTmdb = 1,
        idTmdbRelatedShow = 2,
        updatedAt =
          nowUtcMillis() - (Config.RELATED_CACHE_DURATION + 1000),
      )
      coEvery { showsDao.getAll(any()) } returns emptyList()
      coEvery { showsDao.upsert(any()) } just Runs
      coEvery { catalogApi.fetchRelatedShows(any()) } returns listOf(createShowRemote(2))
      coEvery { relatedShowsDao.getAllById(any()) } returns listOf(showDb)

      val showInput = xyz.stignarnia.ui_model.Show.EMPTY.copy(
        ids = xyz.stignarnia.ui_model.Ids.EMPTY
          .copy(tmdb = xyz.stignarnia.ui_model.IdTmdb(1)),
      )
      SUT.loadAll(showInput, 0)

      coVerifyOrder {
        relatedShowsDao.getAllById(any())
        catalogApi.fetchRelatedShows(any())
      }
      coVerify(exactly = 0) { showsDao.getAll(any()) }
    }
  }
}
