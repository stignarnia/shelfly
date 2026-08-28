package xyz.stignarnia.uiShow.episodes.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

@ViewModelScoped
class EpisodesLoadShowCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val showsRepository: ShowsRepository,
  ) {
    suspend fun loadDetails(idTmdb: IdTmdb) =
      withContext(dispatchers.IO) {
        showsRepository.detailsShow.load(idTmdb)
      }
  }
