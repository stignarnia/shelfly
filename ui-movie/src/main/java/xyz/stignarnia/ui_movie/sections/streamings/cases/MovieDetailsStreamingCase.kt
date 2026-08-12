package xyz.stignarnia.ui_movie.sections.streamings.cases

import xyz.stignarnia.common.ConfigVariant.STREAMINGS_CACHE_DURATION
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.repository.movies.MovieStreamingsRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.common.AppCountry
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.StreamingService
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsStreamingCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val streamingsRepository: MovieStreamingsRepository,
  private val settingsRepository: SettingsRepository,
) {

  suspend fun getLocalStreamingServices(movie: Movie): List<StreamingService> =
    withContext(dispatchers.IO) {
      if (!settingsRepository.streamingsEnabled) {
        return@withContext emptyList()
      }
      val country = AppCountry.fromCode(settingsRepository.country)
      val localData = streamingsRepository.getLocalStreamings(movie, country.code)
      localData.first
    }

  suspend fun loadStreamingServices(movie: Movie): List<StreamingService> =
    withContext(dispatchers.IO) {
      if (!settingsRepository.streamingsEnabled) {
        return@withContext emptyList()
      }
      val country = AppCountry.fromCode(settingsRepository.country)
      val (localItems, timestamp) = streamingsRepository.getLocalStreamings(movie, country.code)
      if (timestamp != null && timestamp.plusSeconds(STREAMINGS_CACHE_DURATION / 1000).isAfter(nowUtc())) {
        return@withContext localItems
      }
      streamingsRepository.loadRemoteStreamings(movie, country.code)
    }
}
