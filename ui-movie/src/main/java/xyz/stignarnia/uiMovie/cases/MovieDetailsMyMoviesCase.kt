package xyz.stignarnia.uiMovie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.Movie
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsMyMoviesCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val moviesRepository: MoviesRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun getAllIds() =
      withContext(dispatchers.IO) {
        val (myMovies, watchlistMovies) =
          awaitAll(
            async { moviesRepository.myMovies.loadAllIds() },
            async { moviesRepository.watchlistMovies.loadAllIds() },
          )
        Pair(myMovies, watchlistMovies)
      }

    suspend fun getMyMovie(movie: Movie): Movie? =
      withContext(dispatchers.IO) {
        moviesRepository.myMovies.load(movie.ids.tmdb)
      }

    suspend fun addToMyMovies(
      movie: Movie,
      customDate: ZonedDateTime?,
    ) {
      withContext(dispatchers.IO) {
        moviesRepository.myMovies.insert(movie.ids.tmdb, customDate)
        pinnedItemsRepository.removePinnedItem(movie)
        announcementManager.refreshMoviesAnnouncements()
      }
    }

    suspend fun removeFromMyMovies(movie: Movie) {
      withContext(dispatchers.IO) {
        moviesRepository.myMovies.delete(movie.ids.tmdb)
        pinnedItemsRepository.removePinnedItem(movie)
      }
    }
  }
