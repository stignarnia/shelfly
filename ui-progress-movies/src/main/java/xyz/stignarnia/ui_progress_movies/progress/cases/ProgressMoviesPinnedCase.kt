package xyz.stignarnia.ui_progress_movies.progress.cases

import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ProgressMoviesPinnedCase @Inject constructor(
  private val pinnedItemsRepository: PinnedItemsRepository,
) {

  fun addPinnedItem(item: Movie) = pinnedItemsRepository.addPinnedItem(item)

  fun removePinnedItem(item: Movie) = pinnedItemsRepository.removePinnedItem(item)
}
