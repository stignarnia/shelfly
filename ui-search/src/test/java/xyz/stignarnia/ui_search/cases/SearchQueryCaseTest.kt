package xyz.stignarnia.ui_search.cases

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.data_remote.catalog.model.SearchResult
import xyz.stignarnia.data_remote.catalog.model.Show
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.movies.MyMoviesRepository
import xyz.stignarnia.repository.movies.WatchlistMoviesRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.MyShowsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.repository.shows.WatchlistShowsRepository
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_search.BaseMockTest
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchQueryCaseTest : BaseMockTest() {

  @RelaxedMockK lateinit var cloud: RemoteDataSource
  @RelaxedMockK lateinit var catalogApi: TmdbRemoteDataSource
  @RelaxedMockK lateinit var mappers: Mappers
  @RelaxedMockK lateinit var preferences: SharedPreferences
  private lateinit var settingsRepository: SettingsRepository
  @RelaxedMockK lateinit var myShows: MyShowsRepository
  @RelaxedMockK lateinit var watchlistShows: WatchlistShowsRepository
  @RelaxedMockK lateinit var myMovies: MyMoviesRepository
  @RelaxedMockK lateinit var watchlistMovies: WatchlistMoviesRepository
  @RelaxedMockK lateinit var translationsRepository: TranslationsRepository
  @RelaxedMockK lateinit var showImagesProvider: ShowImagesProvider
  @RelaxedMockK lateinit var movieImagesProvider: MovieImagesProvider

  private lateinit var showsRepository: ShowsRepository
  private lateinit var moviesRepository: MoviesRepository
  private lateinit var SUT: SearchQueryCase

  @Before
  override fun setUp() {
    super.setUp()

    every { preferences.getBoolean("KEY_MOVIES_ENABLED", true) } returns true
    settingsRepository = SettingsRepository(
      sorting = mockk(),
      filters = mockk(),
      widgets = mockk(),
      viewMode = mockk(),
      spoilers = mockk { every { getAll() } returns xyz.stignarnia.ui_model.SpoilersSettings.INITIAL },
      sync = mockk(),
      webdav = mockk(),
      dispatchers = testDispatchers,
      localSource = mockk(),
      transactions = mockk(),
      mappers = mappers,
      preferences = preferences,
    )

    showsRepository = ShowsRepository(
      discoverShows = mockk(),
      myShows = myShows,
      watchlistShows = watchlistShows,
      hiddenShows = mockk(),
      relatedShows = mockk(),
      detailsShow = mockk(),
    )
    moviesRepository = MoviesRepository(
      discoverMovies = mockk(),
      relatedMovies = mockk(),
      movieDetails = mockk(),
      myMovies = myMovies,
      watchlistMovies = watchlistMovies,
      hiddenMovies = mockk(),
    )

    coEvery { cloud.tmdb } returns catalogApi
    coEvery { translationsRepository.getLanguage() } returns "en"

    coEvery { showImagesProvider.findCachedImage(any(), any()) } returns Image.createUnknown(ImageType.POSTER)
    coEvery { movieImagesProvider.findCachedImage(any(), any()) } returns Image.createUnknown(ImageType.POSTER)

    coEvery { myShows.loadAllIds() } returns emptyList()
    coEvery { watchlistShows.loadAllIds() } returns emptyList()
    coEvery { myMovies.loadAllIds() } returns emptyList()
    coEvery { watchlistMovies.loadAllIds() } returns emptyList()

    SUT = SearchQueryCase(
      testDispatchers,
      cloud,
      mappers,
      settingsRepository,
      showsRepository,
      moviesRepository,
      translationsRepository,
      showImagesProvider,
      movieImagesProvider,
    )
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `Should run search query and return results sorted by order`() =
    runTest {
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
        votes = 10,
        comment_count = null,
        genres = null,
        aired_episodes = null,
      )
      val item1 = SearchResult(order = 3, score = 1F, show = showRemote, movie = null, person = null)
      val item2 = SearchResult(order = 2, score = 2F, show = showRemote, movie = null, person = null)
      val item3 = SearchResult(order = 1, score = 3F, show = showRemote, movie = null, person = null)

      coEvery { catalogApi.fetchSearchResults(any()) } returns listOf(item1, item2, item3)

      val result = SUT.searchByQuery("test")

      assertThat(result).hasSize(3)
      assertThat(result[0].order).isEqualTo(1)
      assertThat(result[1].order).isEqualTo(2)
      assertThat(result[2].order).isEqualTo(3)
      coVerify(exactly = 1) { catalogApi.fetchSearchResults("test") }
      coVerify(exactly = 3) { showImagesProvider.findCachedImage(any(), any()) }
      coVerify(exactly = 0) { movieImagesProvider.findCachedImage(any(), any()) }
    }
}
