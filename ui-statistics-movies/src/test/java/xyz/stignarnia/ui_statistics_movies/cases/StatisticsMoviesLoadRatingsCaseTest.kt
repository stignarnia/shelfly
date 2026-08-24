package xyz.stignarnia.ui_statistics_movies.cases

import BaseMockTest
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.movies.MyMoviesRepository
import xyz.stignarnia.repository.movies.ratings.MoviesRatingsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.UserRating
import xyz.stignarnia.ui_statistics_movies.views.ratings.recycler.StatisticsMoviesRatingItem
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StatisticsMoviesLoadRatingsCaseTest : BaseMockTest() {

  @MockK lateinit var myMovies: MyMoviesRepository
  @MockK lateinit var moviesRatings: MoviesRatingsRepository
  @MockK lateinit var movieImagesProvider: MovieImagesProvider

  private lateinit var moviesRepository: MoviesRepository
  private lateinit var ratingsRepository: RatingsRepository
  private lateinit var SUT: StatisticsMoviesLoadRatingsCase

  @Before
  override fun setUp() {
    super.setUp()

    moviesRepository = MoviesRepository(
      discoverMovies = mockk(),
      relatedMovies = mockk(),
      movieDetails = mockk(),
      myMovies = myMovies,
      watchlistMovies = mockk(),
      hiddenMovies = mockk(),
    )

    ratingsRepository = RatingsRepository(
      shows = mockk(),
      movies = moviesRatings,
    )

    SUT = StatisticsMoviesLoadRatingsCase(
      moviesRepository,
      ratingsRepository,
      movieImagesProvider,
    )
  }

  @Test
  fun `Should load sorted ratings properly`() =
    runTest {
      val ratings = listOf(
        UserRating.EMPTY.copy(IdTmdb(1), ratedAt = ZonedDateTime.of(2000, 3, 1, 1, 1, 1, 1, ZoneId.systemDefault())),
        UserRating.EMPTY.copy(IdTmdb(2), ratedAt = ZonedDateTime.of(2001, 3, 1, 1, 1, 1, 1, ZoneId.systemDefault())),
        UserRating.EMPTY.copy(IdTmdb(3), ratedAt = ZonedDateTime.of(2002, 3, 1, 1, 1, 1, 1, ZoneId.systemDefault())),
      )

      val movies = listOf(
        Movie.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(1))),
        Movie.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(2))),
        Movie.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(3))),
        Movie.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(4))),
        Movie.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(5))),
      )

      val image = Image.createUnknown(ImageType.POSTER)

      coEvery { moviesRatings.loadMoviesRatings() } returns ratings
      coEvery { myMovies.loadAll(any()) } returns movies
      coEvery { movieImagesProvider.findCachedImage(any(), any()) } returns image

      val result = SUT.loadRatings()

      assertThat(result).hasSize(3)
      assertThat(result).containsExactly(
        StatisticsMoviesRatingItem(movies[2], image, false, ratings[2]),
        StatisticsMoviesRatingItem(movies[1], image, false, ratings[1]),
        StatisticsMoviesRatingItem(movies[0], image, false, ratings[0]),
      )
    }
}
