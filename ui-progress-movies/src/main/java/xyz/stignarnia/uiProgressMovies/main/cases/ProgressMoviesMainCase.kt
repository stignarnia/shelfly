package xyz.stignarnia.uiProgressMovies.main.cases

import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Movie
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressMoviesMainCase
  @Inject
  constructor(
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
