package xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.notifications.AnnouncementManager
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuWatchlistCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val showsRepository: ShowsRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun moveToWatchlist(
    tmdbId: IdTmdb,
    removeLocalData: Boolean,
  ) = withContext(dispatchers.IO) {
    val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))

    val (isMyShow, isHidden) = awaitAll(
      async { showsRepository.myShows.exists(tmdbId) },
      async { showsRepository.hiddenShows.exists(tmdbId) },
    )

    transactions.withTransaction {
      showsRepository.watchlistShows.insert(show.ids.tmdb)

      if (removeLocalData && isMyShow) {
        localSource.episodes.deleteAllUnwatchedForShow(tmdbId.id)
        val seasons = localSource.seasons.getAllByShowId(tmdbId.id)
        val episodes = localSource.episodes.getAllByShowId(tmdbId.id)
        val toDelete = mutableListOf<Season>()
        seasons.forEach { season ->
          if (episodes.none { it.idSeason == season.idTmdb }) {
            toDelete.add(season)
          }
        }
        localSource.seasons.delete(toDelete)
      }
    }

    pinnedItemsRepository.removePinnedItem(show)
    announcementManager.refreshShowsAnnouncements()
  }

  suspend fun removeFromWatchlist(tmdbId: IdTmdb) =
    withContext(dispatchers.IO) {
      showsRepository.watchlistShows.delete(tmdbId)
      announcementManager.refreshShowsAnnouncements()
    }
}
