package xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases

import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuPinnedCase @Inject constructor(
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val onHoldItemsRepository: OnHoldItemsRepository,
) {

  fun addToTopPinned(tmdbId: IdTmdb) {
    val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
    onHoldItemsRepository.removeItem(show)
    pinnedItemsRepository.addPinnedItem(show)
  }

  fun removeFromTopPinned(tmdbId: IdTmdb) {
    val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
    pinnedItemsRepository.removePinnedItem(show)
  }
}
