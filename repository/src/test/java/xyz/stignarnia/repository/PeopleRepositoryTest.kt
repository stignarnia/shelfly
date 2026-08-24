package xyz.stignarnia.repository

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.data_local.database.dao.MoviesDao
import xyz.stignarnia.data_local.database.dao.PeopleCreditsDao
import xyz.stignarnia.data_local.database.dao.PeopleDao
import xyz.stignarnia.data_local.database.dao.PeopleShowsMoviesDao
import xyz.stignarnia.data_local.database.dao.ShowsDao
import xyz.stignarnia.data_local.database.model.Movie
import xyz.stignarnia.data_local.database.model.Show
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.data_remote.catalog.model.PersonCredit
import xyz.stignarnia.repository.common.BaseMockTest
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Person
import xyz.stignarnia.ui_model.Person.Department
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.data_local.database.model.Person as PersonDb

class PeopleRepositoryTest : BaseMockTest() {

  private fun createPersonModel(idTmdb: Long = 1) =
    Person(
      ids = Ids.EMPTY.copy(tmdb = IdTmdb(idTmdb)),
      name = "Person $idTmdb",
      department = Department.ACTING,
      bio = null,
      bioTranslation = null,
      characters = emptyList(),
      jobs = emptyList(),
      episodesCount = 0,
      birthplace = null,
      imagePath = null,
      homepage = null,
      birthday = null,
      deathday = null,
    )

  @RelaxedMockK lateinit var peopleDao: PeopleDao
  @RelaxedMockK lateinit var showsDao: ShowsDao
  @RelaxedMockK lateinit var moviesDao: MoviesDao
  @RelaxedMockK lateinit var peopleShowsMoviesDao: PeopleShowsMoviesDao
  @RelaxedMockK lateinit var peopleCreditsDao: PeopleCreditsDao
  private val person = PersonDb(
    idTmdb = 1,
    idImdb = null,
    name = "Person",
    department = "Acting",
    biography = null,
    biographyTranslation = null,
    birthday = null,
    birthplace = null,
    character = null,
    episodesCount = null,
    job = null,
    deathday = null,
    image = "test",
    homepage = null,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
    detailsUpdatedAt = null,
  )
  @RelaxedMockK lateinit var tmdbApi: TmdbRemoteDataSource
  private lateinit var settingsRepository: SettingsRepository

  private lateinit var SUT: PeopleRepository

  @Before
  override fun setUp() {
    super.setUp()
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
      mappers = mappers,
      preferences = mockk(relaxed = true),
    )
    SUT = PeopleRepository(settingsRepository, database, cloud, transactions, mappers)
    coEvery { database.people } returns peopleDao
    coEvery { database.shows } returns showsDao
    coEvery { database.movies } returns moviesDao
    coEvery { database.peopleCredits } returns peopleCreditsDao
    coEvery { database.peopleShowsMovies } returns peopleShowsMoviesDao
    coEvery { cloud.tmdb } returns tmdbApi
  }

  @After
  fun confirmSutVerified() {
    confirmVerified(peopleDao)
  }

  @Test
  fun `Should return local data for shows properly`() =
    runBlocking {
      coEvery { peopleShowsMoviesDao.getTimestampForShow(any()) } returns nowUtc().minusHours(10).toMillis()
      coEvery { peopleDao.getAllForShow(any()) } returns listOf(person)

      SUT.loadAllForShow(Ids.EMPTY.copy(tmdb = IdTmdb(11)))

      coVerifyOrder {
        peopleShowsMoviesDao.getTimestampForShow(11)
        peopleDao.getAllForShow(11)
      }
      coVerify(exactly = 0) { tmdbApi.fetchShowPeople(any()) }
    }

  @Test
  fun `Should return remote data for shows if cache is empty`() =
    runBlocking {
      coEvery { peopleShowsMoviesDao.getTimestampForShow(any()) } returns nowUtc().minusHours(10).toMillis()
      coEvery { peopleDao.getAllForShow(any()) } returns listOf()
      coEvery { tmdbApi.fetchShowPeople(any()) } returns mapOf()
      coEvery { peopleDao.upsert(any()) } just Runs
      coEvery { peopleShowsMoviesDao.insertForShow(any(), any()) } just Runs

      SUT.loadAllForShow(Ids.EMPTY.copy(tmdb = IdTmdb(12)))

      coVerifyOrder {
        peopleShowsMoviesDao.getTimestampForShow(12)
        peopleDao.getAllForShow(12)
        tmdbApi.fetchShowPeople(12)
        peopleDao.upsert(any())
        peopleShowsMoviesDao.insertForShow(any(), 12)
      }
    }

  @Test
  fun `Should return remote data for shows properly`() =
    runBlocking {
      coEvery { peopleShowsMoviesDao.getTimestampForShow(any()) } returns nowUtc().minusDays(10).toMillis()
      coEvery { peopleDao.getAllForShow(any()) } returns listOf(person)

      SUT.loadAllForShow(Ids.EMPTY.copy(tmdb = IdTmdb(12)))

      coVerifyOrder {
        peopleShowsMoviesDao.getTimestampForShow(12)
        peopleDao.getAllForShow(12)
        tmdbApi.fetchShowPeople(12)
        peopleDao.upsert(any())
        peopleShowsMoviesDao.insertForShow(any(), 12)
      }
    }

  @Test
  fun `Should return local data for movies properly`() =
    runBlocking {
      coEvery { peopleShowsMoviesDao.getTimestampForMovie(any()) } returns nowUtc().minusHours(10).toMillis()
      coEvery { peopleDao.getAllForMovie(any()) } returns listOf(person)

      SUT.loadAllForMovie(Ids.EMPTY.copy(tmdb = IdTmdb(11)))

      coVerifyOrder {
        peopleShowsMoviesDao.getTimestampForMovie(11)
        peopleDao.getAllForMovie(11)
      }
      coVerify(exactly = 0) { tmdbApi.fetchMoviePeople(any()) }
    }

  @Test
  fun `Should return remote data for movies if cache is empty`() =
    runBlocking {
      coEvery { peopleShowsMoviesDao.getTimestampForMovie(any()) } returns nowUtc().minusHours(10).toMillis()
      coEvery { peopleDao.getAllForMovie(any()) } returns listOf()
      coEvery { tmdbApi.fetchMoviePeople(any()) } returns mapOf()
      coEvery { peopleDao.upsert(any()) } just Runs
      coEvery { peopleShowsMoviesDao.insertForMovie(any(), any()) } just Runs

      SUT.loadAllForMovie(Ids.EMPTY.copy(tmdb = IdTmdb(12)))

      coVerifyOrder {
        peopleShowsMoviesDao.getTimestampForMovie(12)
        peopleDao.getAllForMovie(12)
        tmdbApi.fetchMoviePeople(12)
        peopleDao.upsert(any())
        peopleShowsMoviesDao.insertForMovie(any(), 12)
      }
    }

  @Test
  fun `Should return remote data for movies properly`() =
    runBlocking {
      coEvery { peopleShowsMoviesDao.getTimestampForMovie(any()) } returns nowUtc().minusDays(10).toMillis()
      coEvery { peopleDao.getAllForMovie(any()) } returns listOf(person)

      SUT.loadAllForMovie(Ids.EMPTY.copy(tmdb = IdTmdb(12)))

      coVerifyOrder {
        peopleShowsMoviesDao.getTimestampForMovie(12)
        peopleDao.getAllForMovie(12)
        tmdbApi.fetchMoviePeople(12)
        peopleDao.upsert(any())
        peopleShowsMoviesDao.insertForMovie(any(), 12)
      }
    }

  @Test
  fun `Should return locally cached credits if cache is valid`() =
    runBlocking {
      val personModel = createPersonModel(1)
      val personDb = person.copy(idTmdb = 1)
      val show = Show(
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
      val movie = Movie(
        idTmdb = 1,
        idImdb = "1",
        idSlug = "1",
        title = "Movie",
        year = 2020,
        overview = "",
        released = "",
        runtime = 90,
        country = "",
        trailer = "",
        language = "",
        homepage = "",
        status = "",
        rating = 5f,
        votes = 10,
        commentCount = 0,
        genres = "",
        updatedAt = 0,
        createdAt = 0,
      )
      coEvery { peopleDao.getById(any()) } returns personDb
      coEvery { peopleCreditsDao.getTimestampForPerson(any()) } returns nowUtcMillis() - 100
      coEvery { peopleCreditsDao.getAllShowsForPerson(any()) } returns listOf(show)
      coEvery { peopleCreditsDao.getAllMoviesForPerson(any()) } returns listOf(movie)

      val result = SUT.loadCredits(personModel)

      assertThat(result).hasSize(2)
      assertThat(result[0].show).isNotNull()
      assertThat(result[1].movie).isNotNull()
      coVerify(exactly = 0) { tmdbApi.fetchPersonCredits(any(), any()) }
    }

  @Test
  fun `Should return remote credits if cache is invalid`() =
    runBlocking {
      val personModel = createPersonModel(1)
      val personDb = person.copy(idTmdb = 1)
      val showRemote = xyz.stignarnia.data_remote.catalog.model.Show(
        ids = xyz.stignarnia.data_remote.catalog.model.Ids(
          tmdb = 1,
          imdb = "tt1",
          slug = null,
          tvdb = null,
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
      val movieRemote = xyz.stignarnia.data_remote.catalog.model.Movie(
        ids = xyz.stignarnia.data_remote.catalog.model.Ids(
          tmdb = 1,
          imdb = "tt1",
          slug = null,
          tvdb = null,
          tvrage = null,
        ),
        title = "Movie",
        year = 2020,
        overview = null,
        released = null,
        runtime = null,
        country = null,
        trailer = null,
        homepage = null,
        status = null,
        rating = null,
        votes = null,
        comment_count = null,
        genres = null,
        language = null,
      )
      val creditsShow =
        PersonCredit(characters = null, episode_count = null, series_regular = null, show = showRemote, movie = null)
      val creditsMovie =
        PersonCredit(characters = null, episode_count = null, series_regular = null, show = null, movie = movieRemote)

      coEvery { peopleDao.getById(any()) } returns personDb
      coEvery { tmdbApi.fetchPersonCredits(any(), any()) } returns listOf(creditsShow, creditsMovie)

      val result = SUT.loadCredits(personModel)

      assertThat(result).hasSize(2)
      assertThat(result[0].show).isNotNull()
      assertThat(result[1].movie).isNotNull()
      coVerify(exactly = 1) { showsDao.upsert(any()) }
      coVerify(exactly = 1) { moviesDao.upsert(any()) }
      coVerify(exactly = 1) { peopleCreditsDao.insertSingle(any(), any()) }
    }
}
