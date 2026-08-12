package xyz.stignarnia.ui_progress_movies.main.cases

import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Movie
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressMoviesMainCase @Inject constructor(
  private val moviesRepository: MoviesRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
) {

  suspend fun addToMyMovies(
    movie: Movie,
    customDate: ZonedDateTime?,
  ) {
    moviesRepository.myMovies.insert(movie.ids.tmdb, customDate)
    pinnedItemsRepository.removePinnedItem(movie)
  }

  suspend fun addToMyMovies(movieId: IdTmdb) {
    addToMyMovies(
      movie = Movie.EMPTY.copy(Ids.EMPTY.copy(tmdb = movieId)),
      customDate = null,
    )
  }
}
