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
