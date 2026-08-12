package xyz.stignarnia.ui_base.common.sheets.context_menu.movie.cases

import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuPinnedCase @Inject constructor(
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
