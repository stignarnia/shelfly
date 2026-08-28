package xyz.stignarnia.uiMovie.sections.ratings.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsRatingCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsRepository: RatingsRepository,
    private val apiKeyProvider: ApiKeyProvider,
  ) {
    fun hasOmdbApiKey() = apiKeyProvider.hasOmdbApiKey()

    suspend fun loadRating(movie: Movie) =
      withContext(dispatchers.IO) {
        ratingsRepository.movies.loadRatings(listOf(movie)).firstOrNull()
      }

    suspend fun loadExternalRatings(movie: Movie) =
      withContext(dispatchers.IO) {
        ratingsRepository.movies.external.loadRatings(movie)
      }
  }
