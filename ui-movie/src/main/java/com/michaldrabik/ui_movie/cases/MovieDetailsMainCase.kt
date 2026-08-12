package com.michaldrabik.ui_movie.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.movies.MoviesRepository
import com.michaldrabik.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsMainCase @Inject constructor(
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
