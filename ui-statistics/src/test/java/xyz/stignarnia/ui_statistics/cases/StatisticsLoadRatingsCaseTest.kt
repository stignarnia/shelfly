package xyz.stignarnia.ui_statistics.cases

import BaseMockTest
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.UserRating
import xyz.stignarnia.ui_statistics.views.ratings.recycler.StatisticsRatingItem
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("EXPERIMENTAL_API_USAGE")
class StatisticsLoadRatingsCaseTest : BaseMockTest() {

  @MockK lateinit var showsRepository: ShowsRepository
  @MockK lateinit var ratingsRepository: RatingsRepository
  @MockK lateinit var showImagesProvider: ShowImagesProvider

  private lateinit var SUT: StatisticsLoadRatingsCase

  @Before
  override fun setUp() {
    super.setUp()

    SUT = StatisticsLoadRatingsCase(
      showsRepository,
      ratingsRepository,
      showImagesProvider,
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

      val shows = listOf(
        Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(1))),
        Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(2))),
        Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(3))),
        Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(4))),
        Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(5))),
      )

      val image = Image.createUnknown(ImageType.POSTER)

      coEvery { ratingsRepository.shows.loadShowsRatings() } returns ratings
      coEvery { showsRepository.myShows.loadAll(any()) } returns shows
      coEvery { showImagesProvider.findCachedImage(any(), any()) } returns image

      val result = SUT.loadRatings()

      assertThat(result).hasSize(3)
      assertThat(result).containsExactly(
        StatisticsRatingItem(shows[2], image, false, ratings[2]),
        StatisticsRatingItem(shows[1], image, false, ratings[1]),
        StatisticsRatingItem(shows[0], image, false, ratings[0]),
      )
    }
}
