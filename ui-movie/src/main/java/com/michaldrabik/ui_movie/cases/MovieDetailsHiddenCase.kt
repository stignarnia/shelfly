package com.michaldrabik.ui_movie.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.PinnedItemsRepository
import com.michaldrabik.repository.movies.MoviesRepository
import com.michaldrabik.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsHiddenCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val moviesRepository: MoviesRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
) {

  suspend fun isHidden(movie: Movie) =
    withContext(dispatchers.IO) {
      moviesRepository.hiddenMovies.exists(movie.ids.tmdb)
    }

  suspend fun addToHidden(movie: Movie) {
    withContext(dispatchers.IO) {
      moviesRepository.hiddenMovies.insert(movie.ids.tmdb)
      pinnedItemsRepository.removePinnedItem(movie)
    }
  }

  suspend fun removeFromHidden(movie: Movie) {
    withContext(dispatchers.IO) {
      moviesRepository.hiddenMovies.delete(movie.ids.tmdb)
      pinnedItemsRepository.removePinnedItem(movie)
    }
  }
}
