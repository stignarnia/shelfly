package xyz.stignarnia.ui_show.cases

import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsListsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val listsRepository: ListsRepository,
) {

  suspend fun getListsCount(show: Show) =
    withContext(dispatchers.IO) {
      listsRepository.loadListIdsForItem(IdTmdb(show.tmdbId), Mode.SHOWS.type).size
    }
}
