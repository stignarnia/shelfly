package xyz.stignarnia.uiMyShows.watchlist.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.Translation
import javax.inject.Inject

@ViewModelScoped
class WatchlistTranslationsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val translationsRepository: TranslationsRepository,
  ) {
    fun getLanguage() = translationsRepository.getLanguage()

    suspend fun loadTranslation(
      show: Show,
      onlyLocal: Boolean,
    ): Translation? =
      withContext(dispatchers.IO) {
        val language = getLanguage()
        if (language == Config.DEFAULT_LANGUAGE) {
          return@withContext Translation.EMPTY
        }
        translationsRepository.loadTranslation(show, language, onlyLocal)
      }
  }
