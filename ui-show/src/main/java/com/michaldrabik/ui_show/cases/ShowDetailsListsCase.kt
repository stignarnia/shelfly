package com.michaldrabik.ui_show.cases

import com.michaldrabik.common.Mode
import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.ListsRepository
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Show
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
