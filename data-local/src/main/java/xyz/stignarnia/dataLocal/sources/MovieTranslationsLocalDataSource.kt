package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.MovieTranslation

interface MovieTranslationsLocalDataSource {
  suspend fun getById(
    tmdbId: Long,
    language: String,
  ): MovieTranslation?

  suspend fun getAll(language: String): List<MovieTranslation>

  suspend fun insertSingle(translation: MovieTranslation)

  suspend fun deleteByLanguage(languages: List<String>)
}
