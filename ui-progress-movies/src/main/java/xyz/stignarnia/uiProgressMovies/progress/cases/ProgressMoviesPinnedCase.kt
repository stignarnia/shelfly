package xyz.stignarnia.uiProgressMovies.progress.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

@ViewModelScoped
class ProgressMoviesPinnedCase
  @Inject
  constructor(
    private val pinnedItemsRepository: PinnedItemsRepository,
  ) {
    fun addPinnedItem(item: Movie) = pinnedItemsRepository.addPinnedItem(item)

    fun removePinnedItem(item: Movie) = pinnedItemsRepository.removePinnedItem(item)
  }
