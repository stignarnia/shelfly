package com.michaldrabik.ui_base.common.sheets.context_menu.movie.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.PinnedItemsRepository
import com.michaldrabik.repository.movies.MoviesRepository
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Ids
import com.michaldrabik.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuHiddenCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val moviesRepository: MoviesRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
) {

  suspend fun moveToHidden(tmdbId: IdTmdb) =
    withContext(dispatchers.IO) {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))

      val (isMyMovie, isWatchlist) = awaitAll(
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
