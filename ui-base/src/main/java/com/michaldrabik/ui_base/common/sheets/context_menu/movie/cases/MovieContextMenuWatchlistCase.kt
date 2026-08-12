package com.michaldrabik.ui_base.common.sheets.context_menu.movie.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.PinnedItemsRepository
import com.michaldrabik.repository.movies.MoviesRepository
import com.michaldrabik.ui_base.common.sheets.context_menu.events.RemoveTraktUiEvent
import com.michaldrabik.ui_base.notifications.AnnouncementManager
import com.michaldrabik.ui_base.trakt.quicksync.QuickSyncManager
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Ids
import com.michaldrabik.ui_model.Movie
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
  private val quickSyncManager: QuickSyncManager,
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

      with(quickSyncManager) {
        clearMovies(listOf(tmdbId.id))
        clearHiddenMovies(listOf(tmdbId.id))
        scheduleMoviesWatchlist(listOf(tmdbId.id))
      }

      RemoveTraktUiEvent(removeProgress = isMyMovie, removeHidden = isHidden)
    }

  suspend fun removeFromWatchlist(tmdbId: IdTmdb) =
    withContext(dispatchers.IO) {
      moviesRepository.watchlistMovies.delete(tmdbId)
      quickSyncManager.clearWatchlistMovies(listOf(tmdbId.id))
    }
}
