package com.michaldrabik.ui_base.common.sheets.context_menu.show.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.Season
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.PinnedItemsRepository
import com.michaldrabik.repository.shows.ShowsRepository
import com.michaldrabik.ui_base.common.sheets.context_menu.events.RemoveTraktUiEvent
import com.michaldrabik.ui_base.notifications.AnnouncementManager
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Ids
import com.michaldrabik.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuHiddenCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val showsRepository: ShowsRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun moveToHidden(
    tmdbId: IdTmdb,
    removeLocalData: Boolean,
  ) = withContext(dispatchers.IO) {
    val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))

    val (isMyShow, isWatchlist) = awaitAll(
      async { showsRepository.myShows.exists(tmdbId) },
      async { showsRepository.watchlistShows.exists(tmdbId) },
    )

    transactions.withTransaction {
      showsRepository.hiddenShows.insert(show.ids.tmdb)

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

    RemoveTraktUiEvent(removeProgress = isMyShow, removeWatchlist = isWatchlist)
  }

  suspend fun removeFromHidden(tmdbId: IdTmdb) =
    withContext(dispatchers.IO) {
      showsRepository.hiddenShows.delete(tmdbId)
      announcementManager.refreshShowsAnnouncements()
    }
}
