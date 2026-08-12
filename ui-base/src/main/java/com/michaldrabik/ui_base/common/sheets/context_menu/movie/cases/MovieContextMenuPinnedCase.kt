package com.michaldrabik.ui_base.common.sheets.context_menu.movie.cases

import com.michaldrabik.repository.PinnedItemsRepository
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Ids
import com.michaldrabik.ui_model.Movie
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
