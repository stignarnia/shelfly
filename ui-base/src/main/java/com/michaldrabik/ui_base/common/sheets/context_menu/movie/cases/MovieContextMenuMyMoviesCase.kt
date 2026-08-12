package com.michaldrabik.ui_base.common.sheets.context_menu.movie.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.PinnedItemsRepository
import com.michaldrabik.repository.movies.MoviesRepository
import com.michaldrabik.ui_base.notifications.AnnouncementManager
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Ids
import com.michaldrabik.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuMyMoviesCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val moviesRepository: MoviesRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun moveToMyMovies(
    tmdbId: IdTmdb,
    customDate: ZonedDateTime? = null,
  ) = withContext(dispatchers.IO) {
    val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))

    val (isWatchlist, isHidden) = awaitAll(
      async { moviesRepository.watchlistMovies.exists(tmdbId) },
      async { moviesRepository.hiddenMovies.exists(tmdbId) },
    )

    moviesRepository.myMovies.insert(tmdbId, customDate)
    pinnedItemsRepository.removePinnedItem(movie)
    announcementManager.refreshMoviesAnnouncements()
  }

  suspend fun removeFromMyMovies(tmdbId: IdTmdb) =
    withContext(dispatchers.IO) {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
      moviesRepository.myMovies.delete(tmdbId)
      pinnedItemsRepository.removePinnedItem(movie)
    }
}
