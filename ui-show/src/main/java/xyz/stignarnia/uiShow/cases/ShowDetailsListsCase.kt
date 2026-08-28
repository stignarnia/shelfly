package xyz.stignarnia.uiShow.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsListsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val listsRepository: ListsRepository,
  ) {
    suspend fun getListsCount(show: Show) =
      withContext(dispatchers.IO) {
        listsRepository.loadListIdsForItem(IdTmdb(show.tmdbId), Mode.SHOWS.type).size
      }
  }
