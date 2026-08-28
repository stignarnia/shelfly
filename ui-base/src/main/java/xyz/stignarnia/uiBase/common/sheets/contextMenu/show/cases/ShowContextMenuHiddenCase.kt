package xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.Season
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuHiddenCase
  @Inject
  constructor(
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

      val (isMyShow, isWatchlist) =
        awaitAll(
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
    }

    suspend fun removeFromHidden(tmdbId: IdTmdb) =
      withContext(dispatchers.IO) {
        showsRepository.hiddenShows.delete(tmdbId)
        announcementManager.refreshShowsAnnouncements()
      }
  }
