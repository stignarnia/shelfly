package xyz.stignarnia.uiStatistics

import BaseMockTest
import TestData
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.shows.HiddenShowsRepository
import xyz.stignarnia.repository.shows.MyShowsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.repository.shows.WatchlistShowsRepository
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageFamily
import xyz.stignarnia.uiModel.ImageSource
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiStatistics.cases.StatisticsLoadRatingsCase
import xyz.stignarnia.uiStatistics.views.mostWatched.StatisticsMostWatchedItem
import xyz.stignarnia.uiStatistics.views.ratings.recycler.StatisticsRatingItem

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest : BaseMockTest() {
  @MockK lateinit var ratingsCase: StatisticsLoadRatingsCase

  @MockK lateinit var myShows: MyShowsRepository

  @MockK lateinit var watchlistShows: WatchlistShowsRepository

  @MockK lateinit var hiddenShows: HiddenShowsRepository

  @MockK lateinit var translationsRepository: TranslationsRepository

  @MockK lateinit var imagesProvider: ShowImagesProvider

  @RelaxedMockK lateinit var database: LocalDataSource

  @RelaxedMockK lateinit var mappers: Mappers

  private lateinit var showsRepository: ShowsRepository
  private lateinit var SUT: StatisticsViewModel

  private val stateResult = mutableListOf<StatisticsUiState>()
  private val messagesResult = mutableListOf<MessageEvent>()

  @Before
  override fun setUp() {
    super.setUp()

    showsRepository =
      ShowsRepository(
        discoverShows = mockk(),
        myShows = myShows,
        watchlistShows = watchlistShows,
        hiddenShows = hiddenShows,
        relatedShows = mockk(),
        detailsShow = mockk(),
      )

    coEvery { translationsRepository.getLanguage() } returns "en"
    coEvery { imagesProvider.findCachedImage(any(), any()) } returns
      Image.createAvailable(
        Ids.EMPTY,
        ImageType.POSTER,
        ImageFamily.SHOW,
        "",
        ImageSource.TMDB,
      )

    SUT =
      StatisticsViewModel(
        ratingsCase,
        showsRepository,
        translationsRepository,
        imagesProvider,
        database,
        mappers,
      )
  }

  @After
  fun tearDown() {
    stateResult.clear()
    messagesResult.clear()
    SUT.viewModelScope.cancel()
  }

  @Test
  internal fun `Should load ratings`() =
    runTest {
      val movieItem = StatisticsRatingItem(Show.EMPTY, Image.createUnknown(ImageType.POSTER), false, UserRating.EMPTY)
      coEvery { ratingsCase.loadRatings() } returns listOf(movieItem)

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }

      SUT.loadRatings()

      assertThat(stateResult.last().ratings?.size).isEqualTo(1)
      assertThat(stateResult.last().ratings).contains(movieItem)

      job.cancel()
    }

  @Test
  internal fun `Should load empty ratings in case of error`() =
    runTest {
      coEvery { ratingsCase.loadRatings() } throws Throwable("Test error")

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }

      SUT.loadRatings()

      assertThat(stateResult.last().ratings).isEmpty()

      job.cancel()
    }

  @Test
  internal fun `Should load statistics properly`() =
    runTest {
      val shows =
        listOf(
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(1)), runtime = 1, genres = listOf("war", "drama")),
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(2)), runtime = 2, genres = listOf("war", "animation")),
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(3)), runtime = 3, genres = listOf("war", "animation")),
        )

      val shows2 =
        listOf(
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(4)), runtime = 1, genres = listOf("war", "drama")),
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(5)), runtime = 2, genres = listOf("war", "animation")),
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(6)), runtime = 3, genres = listOf("war", "animation")),
        )

      val shows3 =
        listOf(
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(7)), runtime = 1, genres = listOf("war", "drama")),
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(8)), runtime = 2, genres = listOf("war", "animation")),
          Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(9)), runtime = 3, genres = listOf("war", "animation")),
        )

      coEvery { myShows.loadAll() } returns shows
      coEvery { watchlistShows.loadAll() } returns shows2
      coEvery { hiddenShows.loadAll() } returns shows3

      coEvery { database.episodes.getAllWatchedForShows(any()) } returns
        listOf(
          TestData.createEpisode().copy(idShowTmdb = 1, runtime = 5),
          TestData.createEpisode().copy(idShowTmdb = 2, runtime = 6),
          TestData.createEpisode().copy(idShowTmdb = 3, runtime = 7),
          TestData.createEpisode().copy(idShowTmdb = 3, runtime = 7),
        )

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }

      SUT.loadData(limit = 0, initialDelay = 0)

      val result = stateResult.last()
      assertThat(result.mostWatchedShows).hasSize(5)
      assertThat(result.mostWatchedTotalCount).isEqualTo(9)
      assertThat(result.totalTimeSpentMinutes).isEqualTo(25)
      assertThat(result.totalWatchedEpisodes).isEqualTo(4)
      assertThat(result.totalWatchedEpisodesShows).isEqualTo(3)
      assertThat(result.topGenres?.size).isEqualTo(3)
      assertThat(result.topGenres).containsExactly(Genre.WAR, Genre.ANIMATION, Genre.DRAMA)

      job.cancel()
    }

  @Test
  internal fun `Should load missing image for most watched item`() =
    runTest {
      val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(1)))
      val item =
        StatisticsMostWatchedItem(
          show = show,
          seasonsCount = 1,
          episodes = emptyList(),
          image = Image.createUnknown(ImageType.POSTER),
          translation = null,
        )
      val loadedImage =
        Image.createAvailable(
          Ids.EMPTY,
          ImageType.POSTER,
          ImageFamily.SHOW,
          "test_path",
          ImageSource.TMDB,
        )
      coEvery { imagesProvider.loadRemoteImage(show, ImageType.POSTER, false) } returns loadedImage
      coEvery { myShows.loadAll() } returns listOf(show)
      coEvery { watchlistShows.loadAll() } returns emptyList()
      coEvery { hiddenShows.loadAll() } returns emptyList()
      coEvery { database.episodes.getAllWatchedForShows(any()) } returns emptyList()

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      SUT.loadData(limit = 0, initialDelay = 0)

      SUT.loadMissingImage(item, false)

      val updatedItem = stateResult.last().mostWatchedShows?.first { it.show.ids.tmdb == show.ids.tmdb }
      assertThat(updatedItem?.image).isEqualTo(loadedImage)

      job.cancel()
    }

  @Test
  internal fun `Should load missing image for rating item`() =
    runTest {
      val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = IdTmdb(1)))
      val ratingItem = StatisticsRatingItem(show, Image.createUnknown(ImageType.POSTER), false, UserRating.EMPTY)
      val loadedImage =
        Image.createAvailable(
          Ids.EMPTY,
          ImageType.POSTER,
          ImageFamily.SHOW,
          "test_path",
          ImageSource.TMDB,
        )
      coEvery { ratingsCase.loadRatings() } returns listOf(ratingItem)
      coEvery { imagesProvider.loadRemoteImage(show, ImageType.POSTER, false) } returns loadedImage

      val job = launch(UnconfinedTestDispatcher()) { SUT.uiState.toList(stateResult) }
      SUT.loadRatings()

      SUT.loadMissingRatingImage(ratingItem, false)

      val updatedItem = stateResult.last().ratings?.first { it.show.ids.tmdb == show.ids.tmdb }
      assertThat(updatedItem?.image).isEqualTo(loadedImage)

      job.cancel()
    }
}
