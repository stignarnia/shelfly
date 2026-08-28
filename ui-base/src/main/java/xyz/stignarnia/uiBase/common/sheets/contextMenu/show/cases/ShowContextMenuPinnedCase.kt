package xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuPinnedCase
  @Inject
  constructor(
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
