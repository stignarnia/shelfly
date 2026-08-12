package xyz.stignarnia.ui_progress_movies.main.cases

import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_progress_movies.BaseMockTest
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@Suppress("EXPERIMENTAL_API_USAGE")
class ProgressMoviesMainCaseTest : BaseMockTest() {

  @RelaxedMockK lateinit var moviesRepository: MoviesRepository
  @RelaxedMockK lateinit var pinnedItemsRepository: PinnedItemsRepository

  private lateinit var SUT: ProgressMoviesMainCase

  @Before
  override fun setUp() {
    super.setUp()
    SUT = ProgressMoviesMainCase(
      moviesRepository,
      pinnedItemsRepository,
    )
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `Should add movie to movies history properly`() =
    runTest {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(123)))

      SUT.addToMyMovies(movie, null)

      coVerify { moviesRepository.myMovies.insert(IdTmdb(123), null) }
      coVerify { pinnedItemsRepository.removePinnedItem(movie) }
    }

  @Test
  fun `Should add movie to movies history properly using only ID`() =
    runTest {
      SUT.addToMyMovies(IdTmdb(123))

      coVerify { moviesRepository.myMovies.insert(IdTmdb(123), null) }
      coVerify { pinnedItemsRepository.removePinnedItem(any<Movie>()) }
    }
}
