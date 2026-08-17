package xyz.stignarnia.ui_backup.features.sync

import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Rating
import xyz.stignarnia.data_local.sources.ArchiveMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.ArchiveShowsLocalDataSource
import xyz.stignarnia.data_local.sources.CustomListsItemsLocalDataSource
import xyz.stignarnia.data_local.sources.CustomListsLocalDataSource
import xyz.stignarnia.data_local.sources.EpisodesLocalDataSource
import xyz.stignarnia.data_local.sources.MoviesLocalDataSource
import xyz.stignarnia.data_local.sources.MyMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.MyShowsLocalDataSource
import xyz.stignarnia.data_local.sources.RatingsLocalDataSource
import xyz.stignarnia.data_local.sources.SeasonsLocalDataSource
import xyz.stignarnia.data_local.sources.WatchlistMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.WatchlistShowsLocalDataSource
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.ui_backup.model.BackupEpisode
import xyz.stignarnia.ui_backup.model.BackupEpisodeRating
import xyz.stignarnia.ui_backup.model.BackupList
import xyz.stignarnia.ui_backup.model.BackupListItem
import xyz.stignarnia.ui_backup.model.BackupLists
import xyz.stignarnia.ui_backup.model.BackupMovie
import xyz.stignarnia.ui_backup.model.BackupMovies
import xyz.stignarnia.ui_backup.model.BackupScheme
import xyz.stignarnia.ui_backup.model.BackupSeason
import xyz.stignarnia.ui_backup.model.BackupSeasonRating
import xyz.stignarnia.ui_backup.model.BackupShow
import xyz.stignarnia.ui_backup.model.BackupShows
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.data_local.database.model.Episode as EpisodeDb
import xyz.stignarnia.data_local.database.model.Season as SeasonDb

/**
 * The applier is the only part of sync that deletes local data, so these cover what it removes as much as what it leaves alone.
 */
class SyncStateApplierTest {

  @MockK lateinit var localSource: LocalDataSource
  @MockK lateinit var transactions: TransactionsProvider

  @RelaxedMockK lateinit var myShows: MyShowsLocalDataSource
  @RelaxedMockK lateinit var watchlistShows: WatchlistShowsLocalDataSource
  @RelaxedMockK lateinit var archiveShows: ArchiveShowsLocalDataSource
  @RelaxedMockK lateinit var myMovies: MyMoviesLocalDataSource
  @RelaxedMockK lateinit var watchlistMovies: WatchlistMoviesLocalDataSource
  @RelaxedMockK lateinit var archiveMovies: ArchiveMoviesLocalDataSource
  @RelaxedMockK lateinit var movies: MoviesLocalDataSource
  @RelaxedMockK lateinit var episodes: EpisodesLocalDataSource
  @RelaxedMockK lateinit var seasons: SeasonsLocalDataSource
  @RelaxedMockK lateinit var ratings: RatingsLocalDataSource
  @RelaxedMockK lateinit var customLists: CustomListsLocalDataSource
  @RelaxedMockK lateinit var customListsItems: CustomListsItemsLocalDataSource
  @RelaxedMockK lateinit var pinnedItems: PinnedItemsRepository
  @RelaxedMockK lateinit var onHoldItems: OnHoldItemsRepository

  private lateinit var SUT: SyncStateApplier

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    clearAllMocks()

    mockkStatic("androidx.room.RoomDatabaseKt")
    val lambda = slot<suspend () -> Unit>()
    coEvery { transactions.withTransaction(capture(lambda)) } coAnswers { lambda.captured.invoke() }

    every { localSource.myShows } returns myShows
    every { localSource.watchlistShows } returns watchlistShows
    every { localSource.archiveShows } returns archiveShows
    every { localSource.myMovies } returns myMovies
    every { localSource.watchlistMovies } returns watchlistMovies
    every { localSource.archiveMovies } returns archiveMovies
    every { localSource.movies } returns movies
    every { localSource.episodes } returns episodes
    every { localSource.seasons } returns seasons
    every { localSource.ratings } returns ratings
    every { localSource.customLists } returns customLists
    every { localSource.customListsItems } returns customListsItems

    SUT = SyncStateApplier(localSource, pinnedItems, onHoldItems, transactions)
  }

  @Test
  fun `Should touch nothing when the merge changed nothing`() =
    runTest {
      val state = scheme(shows = BackupShows(collectionHistory = listOf(show(1))))

      SUT.apply(local = state, merged = state)

      coVerify(exactly = 0) { myShows.deleteById(any()) }
      coVerify(exactly = 0) { transactions.withTransaction<Unit>(any()) }
    }

  @Test
  fun `Should never remove something a peer simply has not heard of`() =
    runTest {
      // The merged state is a superset: absence on one device is not deletion, so a bigger result must still remove nothing.
      val local = scheme(shows = BackupShows(collectionHistory = listOf(show(1))))
      val merged = scheme(shows = BackupShows(collectionHistory = listOf(show(1), show(2))))

      SUT.apply(local, merged)

      coVerify(exactly = 0) { myShows.deleteById(any()) }
    }

  @Test
  fun `Should remove a show dropped from the collection`() =
    runTest {
      val local = scheme(shows = BackupShows(collectionHistory = listOf(show(1), show(2))))
      val merged = scheme(shows = BackupShows(collectionHistory = listOf(show(2))))

      SUT.apply(local, merged)

      coVerify(exactly = 1) { myShows.deleteById(1) }
      coVerify(exactly = 0) { myShows.deleteById(2) }
    }

  @Test
  fun `Should carry both halves of a move between collections`() =
    runTest {
      // Moving a show out of the watchlist and into the collection is a removal and an addition.
      // Only the removal is this class's job, but it has to happen even though the show is still present elsewhere.
      val local = scheme(shows = BackupShows(collectionWatchlist = listOf(show(1))))
      val merged = scheme(shows = BackupShows(collectionHistory = listOf(show(1))))

      SUT.apply(local, merged)

      coVerify(exactly = 1) { watchlistShows.deleteById(1) }
      coVerify(exactly = 0) { myShows.deleteById(any()) }
    }

  @Test
  fun `Should remove movies from every collection they left`() =
    runTest {
      val local = scheme(
        movies = BackupMovies(
          collectionHistory = listOf(movie(10)),
          collectionWatchlist = listOf(movie(11)),
          collectionHidden = listOf(movie(12)),
        ),
      )

      SUT.apply(local, scheme())

      coVerify(exactly = 1) { myMovies.deleteById(10) }
      coVerify(exactly = 1) { watchlistMovies.deleteById(11) }
      coVerify(exactly = 1) { archiveMovies.deleteById(12) }
    }

  @Test
  fun `Should remove a season rating by show and season number`() =
    runTest {
      val local = scheme(
        shows = BackupShows(
          ratingsSeasons = listOf(
            BackupSeasonRating(showTmdbId = 1, seasonNumber = 2, rating = 8, ratedAt = DATE),
            BackupSeasonRating(showTmdbId = 1, seasonNumber = 3, rating = 9, ratedAt = DATE),
          ),
        ),
      )
      val merged = scheme(
        shows = BackupShows(
          ratingsSeasons = listOf(
            BackupSeasonRating(showTmdbId = 1, seasonNumber = 3, rating = 9, ratedAt = DATE),
          ),
        ),
      )

      SUT.apply(local, merged)

      coVerify(exactly = 1) { ratings.deleteByKey(1, Rating.TYPE_SEASON, 2, Rating.NO_NUMBER) }
      coVerify(exactly = 0) { ratings.deleteByKey(1, Rating.TYPE_SEASON, 3, any()) }
    }

  @Test
  fun `Should remove an episode rating by show season and episode number`() =
    runTest {
      val local = scheme(
        shows = BackupShows(
          ratingsEpisodes = listOf(
            BackupEpisodeRating(showTmdbId = 1, seasonNumber = 2, episodeNumber = 5, rating = 7, ratedAt = DATE),
          ),
        ),
      )

      SUT.apply(local, scheme())

      coVerify(exactly = 1) { ratings.deleteByKey(1, Rating.TYPE_EPISODE, 2, 5) }
    }

  @Test
  fun `Should clear watched marks but keep the episode when the show is still followed`() =
    runTest {
      val episode = episodeDb(showId = 1, seasonNumber = 2, episodeNumber = 5, isWatched = true)
      coEvery { episodes.getAllByShowId(1) } returns listOf(episode)
      coEvery { episodes.getAllForSeason(any()) } returns listOf(episode.copy(isWatched = false))
      coEvery { seasons.getAllByShowId(1) } returns listOf(seasonDb(showId = 1, number = 2, isWatched = true))
      coEvery { myShows.checkExists(1) } returns true

      SUT.apply(local = scheme(shows = BackupShows(progressEpisodes = listOf(watched(1, 2, 5)))), merged = scheme())

      val updated = slot<List<EpisodeDb>>()
      coVerify(exactly = 1) { episodes.upsert(capture(updated)) }
      coVerify(exactly = 0) { episodes.delete(any()) }
      with(updated.captured.single()) {
        assertThat(isWatched).isFalse()
        assertThat(lastWatchedAt).isNull()
        assertThat(lastExportedAt).isNull()
      }
    }

  @Test
  fun `Should delete the episode row when the show is no longer followed`() =
    runTest {
      // For an unfollowed show the episodes table is only a cache, which is what EpisodesManager does on the same transition.
      val episode = episodeDb(showId = 1, seasonNumber = 2, episodeNumber = 5, isWatched = true)
      coEvery { episodes.getAllByShowId(1) } returns listOf(episode)
      coEvery { episodes.getAllForSeason(any()) } returns emptyList()
      coEvery { seasons.getAllByShowId(1) } returns listOf(seasonDb(showId = 1, number = 2, isWatched = true))
      coEvery { myShows.checkExists(1) } returns false

      SUT.apply(local = scheme(shows = BackupShows(progressEpisodes = listOf(watched(1, 2, 5)))), merged = scheme())

      coVerify(exactly = 1) { episodes.delete(listOf(episode)) }
      coVerify(exactly = 0) { episodes.upsert(any()) }
    }

  @Test
  fun `Should unwatch a season whose episodes are no longer all watched`() =
    runTest {
      val episode = episodeDb(showId = 1, seasonNumber = 2, episodeNumber = 5, isWatched = true)
      coEvery { episodes.getAllByShowId(1) } returns listOf(episode)
      // After the unwatch the season holds one watched episode out of two.
      coEvery { episodes.getAllForSeason(SEASON_ID) } returns listOf(
        episode.copy(isWatched = false),
        episode.copy(idTmdb = 99, episodeNumber = 6, isWatched = true),
      )
      coEvery { seasons.getAllByShowId(1) } returns listOf(
        seasonDb(showId = 1, number = 2, isWatched = true, episodesCount = 2),
      )
      coEvery { myShows.checkExists(1) } returns true

      SUT.apply(local = scheme(shows = BackupShows(progressEpisodes = listOf(watched(1, 2, 5)))), merged = scheme())

      val updated = slot<List<SeasonDb>>()
      coVerify(exactly = 1) { seasons.update(capture(updated)) }
      assertThat(updated.captured.single().isWatched).isFalse()
    }

  @Test
  fun `Should unwatch a season dropped from watched seasons`() =
    runTest {
      coEvery { seasons.getAllByShowId(1) } returns listOf(seasonDb(showId = 1, number = 2, isWatched = true))

      SUT.apply(
        local = scheme(shows = BackupShows(progressSeasons = listOf(BackupSeason(showTmdbId = 1, seasonNumber = 2)))),
        merged = scheme(),
      )

      val updated = slot<List<SeasonDb>>()
      coVerify(exactly = 1) { seasons.update(capture(updated)) }
      assertThat(updated.captured.single().isWatched).isFalse()
    }

  @Test
  fun `Should remove a list item without removing its list`() =
    runTest {
      val local = scheme(lists = BackupLists(listOf(list(7, items = listOf(listItem(7, 100), listItem(7, 200))))))
      val merged = scheme(lists = BackupLists(listOf(list(7, items = listOf(listItem(7, 200))))))

      SUT.apply(local, merged)

      coVerify(exactly = 1) { customListsItems.deleteItem(idList = 7, idTmdb = 100, type = "show") }
      coVerify(exactly = 0) { customLists.deleteById(any()) }
    }

  @Test
  fun `Should remove a deleted list`() =
    runTest {
      val local = scheme(lists = BackupLists(listOf(list(7, items = listOf(listItem(7, 100))))))

      SUT.apply(local, scheme())

      coVerify(exactly = 1) { customLists.deleteById(7) }
    }

  @Test
  fun `Should remove pinned and on hold entries outside the transaction`() =
    runTest {
      val local = scheme(
        shows = BackupShows(progressPinned = listOf(1), progressOnHold = listOf(2)),
        movies = BackupMovies(progressPinned = listOf(3)),
      )

      SUT.apply(local, scheme())

      coVerify(exactly = 1) { pinnedItems.removeShowPinnedItem(IdTmdb(1)) }
      coVerify(exactly = 1) { onHoldItems.removeItem(IdTmdb(2)) }
      coVerify(exactly = 1) { pinnedItems.removeMoviePinnedItem(IdTmdb(3)) }
    }

  @Test
  fun `Should skip an episode the local database no longer has`() =
    runTest {
      coEvery { episodes.getAllByShowId(1) } returns emptyList()

      SUT.apply(local = scheme(shows = BackupShows(progressEpisodes = listOf(watched(1, 2, 5)))), merged = scheme())

      coVerify(exactly = 0) { episodes.upsert(any()) }
      coVerify(exactly = 0) { episodes.delete(any()) }
      coVerify(exactly = 0) { seasons.update(any()) }
    }

  // Fixtures

  private fun scheme(
    shows: BackupShows = BackupShows(),
    movies: BackupMovies = BackupMovies(),
    lists: BackupLists = BackupLists(),
  ) = BackupScheme(version = 3, platform = "android", createdAt = DATE, shows = shows, movies = movies, lists = lists)

  private fun show(id: Long) = BackupShow(tmdbId = id, title = "Show $id", addedAt = DATE, updatedAt = DATE)

  private fun movie(id: Long) = BackupMovie(tmdbId = id, title = "Movie $id", addedAt = DATE)

  private fun watched(
    showId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
  ) = BackupEpisode(
    showTmdbId = showId,
    episodeNumber = episodeNumber,
    seasonNumber = seasonNumber,
    addedAt = DATE,
  )

  private fun list(
    id: Long,
    items: List<BackupListItem>,
  ) = BackupList(
    id = id,
    slugId = "list-$id",
    name = "List $id",
    description = null,
    privacy = "private",
    itemCount = items.size.toLong(),
    createdAt = DATE,
    updatedAt = DATE,
    items = items,
  )

  private fun listItem(
    listId: Long,
    tmdbId: Long,
  ) = BackupListItem(
    id = tmdbId,
    listId = listId,
    tmdbId = tmdbId,
    type = "show",
    rank = 0,
    listedAt = DATE,
    createdAt = DATE,
    updatedAt = DATE,
  )

  private fun episodeDb(
    showId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
    isWatched: Boolean,
  ) = EpisodeDb(
    idTmdb = 1000L + episodeNumber,
    idSeason = SEASON_ID,
    idShowTmdb = showId,
    idShowTvdb = -1,
    idShowImdb = "",
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    episodeNumberAbs = null,
    episodeOverview = "",
    title = "Episode $episodeNumber",
    firstAired = null,
    commentsCount = 0,
    rating = 0F,
    runtime = 0,
    votesCount = 0,
    isWatched = isWatched,
    lastExportedAt = null,
    lastWatchedAt = null,
  )

  private fun seasonDb(
    showId: Long,
    number: Int,
    isWatched: Boolean,
    episodesCount: Int = 1,
  ) = SeasonDb(
    idTmdb = SEASON_ID,
    idShowTmdb = showId,
    seasonNumber = number,
    seasonTitle = "Season $number",
    seasonOverview = "",
    seasonFirstAired = null,
    episodesCount = episodesCount,
    episodesAiredCount = episodesCount,
    rating = null,
    isWatched = isWatched,
  )

  private companion object {
    const val DATE = "2026-01-01T00:00:00.000Z"
    const val SEASON_ID = 500L
  }
}
