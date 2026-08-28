package xyz.stignarnia.uiMovie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Translation
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsTranslationCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val translationsRepository: TranslationsRepository,
  ) {
    suspend fun loadTranslation(movie: Movie): Translation? =
      withContext(dispatchers.IO) {
        val language = translationsRepository.getLanguage()
        if (language == DEFAULT_LANGUAGE) {
          return@withContext null
        }
        translationsRepository.loadTranslation(movie, language)
      }
  }
