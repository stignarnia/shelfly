package xyz.stignarnia.uiMovie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsWatchlistCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val moviesRepository: MoviesRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun isWatchlist(movie: Movie) =
      withContext(dispatchers.IO) {
        moviesRepository.watchlistMovies.load(movie.ids.tmdb) != null
      }

    suspend fun addToWatchlist(movie: Movie) {
      withContext(dispatchers.IO) {
        moviesRepository.watchlistMovies.insert(movie.ids.tmdb)
        pinnedItemsRepository.removePinnedItem(movie)
        announcementManager.refreshMoviesAnnouncements()
      }
    }

    suspend fun removeFromWatchlist(movie: Movie) {
      withContext(dispatchers.IO) {
        moviesRepository.watchlistMovies.delete(movie.ids.tmdb)
        pinnedItemsRepository.removePinnedItem(movie)
      }
    }
  }
