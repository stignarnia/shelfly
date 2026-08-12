package xyz.stignarnia.ui_my_shows.myshows.cases

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.Translation
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MyShowsTranslationsCase @Inject constructor(
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
