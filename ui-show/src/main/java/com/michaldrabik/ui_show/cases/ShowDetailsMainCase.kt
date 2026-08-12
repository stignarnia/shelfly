package com.michaldrabik.ui_show.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.shows.ShowsRepository
import com.michaldrabik.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsMainCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val showsRepository: ShowsRepository,
) {

  suspend fun loadDetails(idTmdb: IdTmdb) =
    withContext(dispatchers.IO) {
      showsRepository.detailsShow.load(idTmdb)
    }

  suspend fun removeMalformedShow(idTmdb: IdTmdb) =
    withContext(dispatchers.IO) {
      with(showsRepository) {
        myShows.delete(idTmdb)
        watchlistShows.delete(idTmdb)
        hiddenShows.delete(idTmdb)
        detailsShow.delete(idTmdb)
      }
      Timber.d("Removing malformed show...")
    }
}
