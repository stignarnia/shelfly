package xyz.stignarnia.ui_base.common.sheets.context_menu.movie.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.ui_base.notifications.AnnouncementManager
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuWatchlistCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val moviesRepository: MoviesRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun moveToWatchlist(tmdbId: IdTmdb) =
    withContext(dispatchers.IO) {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))

      val (isMyMovie, isHidden) = awaitAll(
        async { moviesRepository.myMovies.exists(tmdbId) },
        async { moviesRepository.hiddenMovies.exists(tmdbId) },
      )

      moviesRepository.watchlistMovies.insert(movie.ids.tmdb)
      pinnedItemsRepository.removePinnedItem(movie)
      announcementManager.refreshMoviesAnnouncements()
    }

  suspend fun removeFromWatchlist(tmdbId: IdTmdb) =
    withContext(dispatchers.IO) {
      moviesRepository.watchlistMovies.delete(tmdbId)
    }
}
