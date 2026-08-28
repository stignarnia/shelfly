package xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuHiddenCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val moviesRepository: MoviesRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
  ) {
    suspend fun moveToHidden(tmdbId: IdTmdb) =
      withContext(dispatchers.IO) {
        val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))

        val (isMyMovie, isWatchlist) =
          awaitAll(
            async { moviesRepository.myMovies.exists(tmdbId) },
            async { moviesRepository.watchlistMovies.exists(tmdbId) },
          )

        moviesRepository.hiddenMovies.insert(movie.ids.tmdb)
        pinnedItemsRepository.removePinnedItem(movie)
      }

    suspend fun removeFromHidden(tmdbId: IdTmdb) =
      withContext(dispatchers.IO) {
        moviesRepository.hiddenMovies.delete(tmdbId)
      }
  }
