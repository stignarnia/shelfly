package xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.ui_base.notifications.AnnouncementManager
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuOnHoldCase @Inject constructor(
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
