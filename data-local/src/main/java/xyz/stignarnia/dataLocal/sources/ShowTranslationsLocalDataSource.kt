package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.ShowTranslation

interface ShowTranslationsLocalDataSource {
  suspend fun getById(
    tmdbId: Long,
    language: String,
  ): ShowTranslation?

  suspend fun getAll(language: String): List<ShowTranslation>

  suspend fun insertSingle(translation: ShowTranslation)

  suspend fun deleteByLanguage(languages: List<String>)
}
