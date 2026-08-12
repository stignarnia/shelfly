package com.michaldrabik.ui_show.episodes.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.shows.ShowsRepository
import com.michaldrabik.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class EpisodesLoadShowCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val showsRepository: ShowsRepository,
) {

  suspend fun loadDetails(idTmdb: IdTmdb) =
    withContext(dispatchers.IO) {
      showsRepository.detailsShow.load(idTmdb)
    }
}
