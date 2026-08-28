package xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Movie
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuMyMoviesCase
  @Inject
  constructor(
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

      val (isWatchlist, isHidden) =
        awaitAll(
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
