package xyz.stignarnia.uiMovie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsHiddenCase
  @Inject
  constructor(
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
