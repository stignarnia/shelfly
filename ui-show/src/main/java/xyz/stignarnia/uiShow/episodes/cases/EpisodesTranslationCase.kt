package xyz.stignarnia.uiShow.episodes.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.SeasonTranslation
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class EpisodesTranslationCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val translationsRepository: TranslationsRepository,
  ) {
    suspend fun loadTranslations(
      season: Season?,
      show: Show,
    ): List<SeasonTranslation> =
      withContext(dispatchers.IO) {
        if (season == null) {
          return@withContext emptyList()
        }

        val language = translationsRepository.getLanguage()
        if (language == DEFAULT_LANGUAGE) {
          return@withContext emptyList()
        }

        translationsRepository.loadTranslations(season, show.ids.tmdb, language)
      }
  }
