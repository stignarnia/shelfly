package xyz.stignarnia.ui_show.sections.nextepisode.cases

import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.Translation
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsTranslationCase @Inject constructor(
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
