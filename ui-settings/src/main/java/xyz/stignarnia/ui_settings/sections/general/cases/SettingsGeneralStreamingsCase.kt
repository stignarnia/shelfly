package xyz.stignarnia.ui_settings.sections.general.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.movies.MovieStreamingsRepository
import xyz.stignarnia.repository.shows.ShowStreamingsRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class SettingsGeneralStreamingsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val showStreamingsRepository: ShowStreamingsRepository,
  private val movieStreamingsRepository: MovieStreamingsRepository,
) {

  suspend fun deleteCache() =
    withContext(dispatchers.IO) {
      showStreamingsRepository.deleteCache()
      movieStreamingsRepository.deleteCache()
    }
}
