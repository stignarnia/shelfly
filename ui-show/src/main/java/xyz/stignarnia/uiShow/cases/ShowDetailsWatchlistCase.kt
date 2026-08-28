package xyz.stignarnia.uiShow.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsWatchlistCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val showsRepository: ShowsRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun isWatchlist(show: Show) =
      withContext(dispatchers.IO) {
        showsRepository.watchlistShows.exists(show.ids.tmdb)
      }

    suspend fun addToWatchlist(show: Show) =
      withContext(dispatchers.IO) {
        showsRepository.watchlistShows.insert(show.ids.tmdb)
        pinnedItemsRepository.removePinnedItem(show)
        announcementManager.refreshShowsAnnouncements()
      }

    suspend fun removeFromWatchlist(show: Show) =
      withContext(dispatchers.IO) {
        showsRepository.watchlistShows.delete(show.ids.tmdb)
        pinnedItemsRepository.removePinnedItem(show)
        announcementManager.refreshShowsAnnouncements()
      }
  }
