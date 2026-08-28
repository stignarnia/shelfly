package xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuOnHoldCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val onHoldItemsRepository: OnHoldItemsRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun addToOnHold(tmdbId: IdTmdb) {
      val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
      pinnedItemsRepository.removePinnedItem(show)
      onHoldItemsRepository.addItem(show)
      withContext(dispatchers.IO) {
        announcementManager.refreshShowsAnnouncements()
      }
    }

    suspend fun removeFromOnHold(tmdbId: IdTmdb) {
      val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
      onHoldItemsRepository.removeItem(show)
      withContext(dispatchers.IO) {
        announcementManager.refreshShowsAnnouncements()
      }
    }
  }
