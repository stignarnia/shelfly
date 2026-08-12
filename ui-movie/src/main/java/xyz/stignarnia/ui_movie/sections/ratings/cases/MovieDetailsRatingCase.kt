package xyz.stignarnia.ui_movie.sections.ratings.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsRatingCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  suspend fun loadRating(movie: Movie) =
    withContext(dispatchers.IO) {
      ratingsRepository.movies.loadRatings(listOf(movie)).firstOrNull()
    }

  suspend fun loadExternalRatings(movie: Movie) =
    withContext(dispatchers.IO) {
      ratingsRepository.movies.external.loadRatings(movie)
    }
}
