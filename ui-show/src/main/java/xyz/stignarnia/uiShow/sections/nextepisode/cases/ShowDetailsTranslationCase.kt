package xyz.stignarnia.uiShow.sections.nextepisode.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.Translation
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsTranslationCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val translationsRepository: TranslationsRepository,
  ) {
    suspend fun loadTranslation(
      episode: Episode,
      show: Show,
      onlyLocal: Boolean = false,
    ): Translation? =
      withContext(dispatchers.IO) {
        val language = translationsRepository.getLanguage()
        if (language == DEFAULT_LANGUAGE) {
          return@withContext null
        }
        translationsRepository.loadTranslation(episode, show.ids.tmdb, language, onlyLocal)
      }
  }
