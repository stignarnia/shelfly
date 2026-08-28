package xyz.stignarnia.uiStatistics.cases

import BaseMockTest
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.shows.MyShowsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.repository.shows.ratings.ShowsRatingsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiStatistics.views.ratings.recycler.StatisticsRatingItem
import java.time.ZoneId
import java.time.ZonedDateTime

class StatisticsLoadRatingsCaseTest : BaseMockTest() {
  @MockK lateinit var myShows: MyShowsRepository

  @MockK lateinit var showsRatings: ShowsRatingsRepository

  @MockK lateinit var showImagesProvider: ShowImagesProvider

  private lateinit var showsRepository: ShowsRepository
  private lateinit var ratingsRepository: RatingsRepository
  private lateinit var SUT: StatisticsLoadRatingsCase

  @Before
  override fun setUp() {
    super.setUp()

    showsRepository =
      ShowsRepository(
        discoverShows = mockk(),
        myShows = myShows,
        watchlistShows = mockk(),
        hiddenShows = mockk(),
        relatedShows = mockk(),
        detailsShow = mockk(),
      )

    ratingsRepository =
      RatingsRepository(
        shows = showsRatings,
        movies = mockk(),
      )

    SUT =
      StatisticsLoadRatingsCase(
        showsRepository,
        ratingsRepository,
        showImagesProvider,
      )
  }

  @Test
  fun `Should load sorted ratings properly`() =
    runTest {
      val ratings =
        listOf(
          UserRating.EMPTY.copy(IdTmdb(1), ratedAt = ZonedDateTime.of(2000, 3, 1, 1, 1, 1, 1, ZoneId.systemDefault())),
          UserRating.EMPTY.copy(IdTmdb(2), ratedAt = ZonedDateTime.of(2001, 3, 1, 1, 1, 1, 1, ZoneId.systemDefault())),
          UserRating.EMPTY.copy(IdTmdb(3), ratedAt = ZonedDateTime.of(2002, 3, 1, 1, 1, 1, 1, ZoneId.systemDefault())),
        )

      val shows =
        listOf(
          Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(1))),
          Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(2))),
          Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(3))),
          Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(4))),
          Show.EMPTY.copy(Ids.EMPTY.copy(tmdb = IdTmdb(5))),
        )

      val image = Image.createUnknown(ImageType.POSTER)

      coEvery { showsRatings.loadShowsRatings() } returns ratings
      coEvery { myShows.loadAll(any()) } returns shows
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
