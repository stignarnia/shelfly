package xyz.stignarnia.uiMovie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsMainCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val moviesRepository: MoviesRepository,
  ) {
    suspend fun loadDetails(idTmdb: IdTmdb) =
      withContext(dispatchers.IO) {
        moviesRepository.movieDetails.load(idTmdb)
      }

    suspend fun removeMalformedMovie(idTmdb: IdTmdb) {
      withContext(dispatchers.IO) {
        with(moviesRepository) {
          myMovies.delete(idTmdb)
          watchlistMovies.delete(idTmdb)
          movieDetails.delete(idTmdb)
        }
      }
      Timber.d("Removing malformed movie...")
    }
  }
