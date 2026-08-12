package xyz.stignarnia.ui_search.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_search.recycler.SearchListItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class SearchInvalidateItemCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val showsRepository: ShowsRepository,
  private val moviesRepository: MoviesRepository,
) {

  suspend fun checkFollowedState(item: SearchListItem) =
    withContext(dispatchers.IO) {
      when {
        item.isShow -> {
          val (isMy, isWatchlist) = awaitAll(
            async { showsRepository.myShows.exists(item.show.ids.tmdb) },
            async { showsRepository.watchlistShows.exists(item.show.ids.tmdb) },
          )
          Pair(isMy, isWatchlist)
        }
        item.isMovie -> {
          val (isMy, isWatchlist) = awaitAll(
            async { moviesRepository.myMovies.exists(item.movie.ids.tmdb) },
            async { moviesRepository.watchlistMovies.exists(item.movie.ids.tmdb) },
          )
          Pair(isMy, isWatchlist)
        }
        else -> {
          throw IllegalStateException()
        }
      }
    }
}
