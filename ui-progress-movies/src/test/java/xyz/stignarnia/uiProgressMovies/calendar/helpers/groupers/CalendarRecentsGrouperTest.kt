package xyz.stignarnia.uiProgressMovies.calendar.helpers.groupers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiProgressMovies.BaseMockTest
import xyz.stignarnia.uiProgressMovies.R
import xyz.stignarnia.uiProgressMovies.calendar.recycler.CalendarMovieListItem
import java.time.LocalDate
import java.time.ZonedDateTime

class CalendarRecentsGrouperTest : BaseMockTest() {
  private lateinit var SUT: CalendarRecentsGrouper

  private fun createMovieItem(movie: Movie) =
    CalendarMovieListItem.MovieItem(
      movie = movie,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
      isWatched = false,
      isWatchlist = false,
      translation = null,
      dateFormat = null,
      spoilers = SpoilersSettings.INITIAL,
    )

  @Before
  override fun setUp() {
    super.setUp()
    SUT = CalendarRecentsGrouper()
  }

  @Test
  fun `Should group past items by time properly`() {
    val zonedNow = ZonedDateTime.parse("2021-12-20T12:00:00Z")
    val now = LocalDate.parse("2021-12-20") // Monday
    val movie1 = Movie.EMPTY.copy(released = now.minusDays(1))
    val movie2 = Movie.EMPTY.copy(released = now.minusDays(7))
    val movie3 = Movie.EMPTY.copy(released = now.minusDays(30))
    val movie4 = Movie.EMPTY.copy(released = now.minusDays(90))

    val item1 = createMovieItem(movie1)
    val item2 = createMovieItem(movie2)
    val item3 = createMovieItem(movie3)
    val item4 = createMovieItem(movie4)

    val results = SUT.groupByTime(zonedNow, listOf(item1, item2, item3, item4))

    assertThat(results).hasSize(8)
    assertThat((results[0] as CalendarMovieListItem.Header).textResId).isEqualTo(R.string.textYesterday)
    assertThat((results[1] as CalendarMovieListItem.MovieItem).movie).isEqualTo(movie1)
    assertThat((results[2] as CalendarMovieListItem.Header).textResId).isEqualTo(R.string.textLast7Days)
    assertThat((results[3] as CalendarMovieListItem.MovieItem).movie).isEqualTo(movie2)
    assertThat((results[4] as CalendarMovieListItem.Header).textResId).isEqualTo(R.string.textLast30Days)
    assertThat((results[5] as CalendarMovieListItem.MovieItem).movie).isEqualTo(movie3)
    assertThat((results[6] as CalendarMovieListItem.Header).textResId).isEqualTo(R.string.textLast90Days)
    assertThat((results[7] as CalendarMovieListItem.MovieItem).movie).isEqualTo(movie4)
  }

  @Test
  fun `Should not include items older than 90 days`() {
    val zonedNow = ZonedDateTime.parse("2021-12-20T18:00:00Z")
    val now = LocalDate.parse("2021-12-20") // Monday
    val movie1 = Movie.EMPTY.copy(released = now.minusDays(91))

    val item1 = createMovieItem(movie1)

    val results = SUT.groupByTime(zonedNow, listOf(item1))

    assertThat(results).isEmpty()
  }
}
