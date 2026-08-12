package xyz.stignarnia.ui_movie.cases

import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsListsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val listsRepository: ListsRepository,
) {

  suspend fun countLists(movie: Movie) =
    withContext(dispatchers.IO) {
      listsRepository.loadListIdsForItem(IdTmdb(movie.tmdbId), Mode.MOVIES.type).size
    }
}
