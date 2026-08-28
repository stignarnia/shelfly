package xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuPinnedCase
  @Inject
  constructor(
    private val pinnedItemsRepository: PinnedItemsRepository,
  ) {
    fun addToTopPinned(tmdbId: IdTmdb) {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
      pinnedItemsRepository.addPinnedItem(movie)
    }

    fun removeFromTopPinned(tmdbId: IdTmdb) {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
      pinnedItemsRepository.removePinnedItem(movie)
    }
  }
