package xyz.stignarnia.uiShow.sections.ratings.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsRatingCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsRepository: RatingsRepository,
    private val apiKeyProvider: ApiKeyProvider,
  ) {
    fun hasOmdbApiKey() = apiKeyProvider.hasOmdbApiKey()

    suspend fun loadRating(show: Show) =
      withContext(dispatchers.IO) {
        ratingsRepository.shows.loadRatings(listOf(show)).firstOrNull()
      }

    suspend fun loadExternalRatings(show: Show) =
      withContext(dispatchers.IO) {
        ratingsRepository.shows.external.loadRatings(show)
      }
  }
