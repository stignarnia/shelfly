package xyz.stignarnia.uiMovie.sections.related.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsRelatedCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val moviesRepository: MoviesRepository,
  ) {
    // TODO Add Hidden items
    suspend fun loadRelatedMovies(movie: Movie): List<Movie> =
      withContext(dispatchers.IO) {
        moviesRepository.relatedMovies
          .loadAll(movie)
          .sortedWith(compareBy({ it.votes }, { it.rating }))
          .reversed()
      }
  }
