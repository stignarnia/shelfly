package xyz.stignarnia.ui_show.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.notifications.AnnouncementManager
import xyz.stignarnia.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsHiddenCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val showsRepository: ShowsRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun isHidden(show: Show) =
    withContext(dispatchers.IO) {
      showsRepository.hiddenShows.exists(show.ids.tmdb)
    }

  suspend fun addToHidden(
    show: Show,
    removeLocalData: Boolean,
  ) = withContext(dispatchers.IO) {
    transactions.withTransaction {
      showsRepository.hiddenShows.insert(show.ids.tmdb)

      if (removeLocalData) {
        localSource.episodes.deleteAllUnwatchedForShow(show.tmdbId)
        val seasons = localSource.seasons.getAllByShowId(show.tmdbId)
        val episodes = localSource.episodes.getAllByShowId(show.tmdbId)
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

  suspend fun removeFromHidden(show: Show) =
    withContext(dispatchers.IO) {
      showsRepository.hiddenShows.delete(show.ids.tmdb)
      pinnedItemsRepository.removePinnedItem(show)
      announcementManager.refreshShowsAnnouncements()
    }
}
