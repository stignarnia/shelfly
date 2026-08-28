package xyz.stignarnia.uiSearch.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.Translation
import javax.inject.Inject

@ViewModelScoped
class SearchTranslationsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val translationsRepository: TranslationsRepository,
  ) {
    fun getLanguage() = translationsRepository.getLanguage()

    suspend fun loadTranslation(show: Show): Translation? =
      withContext(dispatchers.IO) {
        val language = getLanguage()
        if (language == Config.DEFAULT_LANGUAGE) {
          return@withContext Translation.EMPTY
        }
        translationsRepository.loadTranslation(show, language)
      }

    suspend fun loadTranslation(movie: Movie): Translation? =
      withContext(dispatchers.IO) {
        val language = getLanguage()
        if (language == Config.DEFAULT_LANGUAGE) {
          return@withContext Translation.EMPTY
        }
        translationsRepository.loadTranslation(movie, language)
      }
  }
