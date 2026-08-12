package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.MovieTranslation

interface MovieTranslationsLocalDataSource {

  suspend fun getById(
    tmdbId: Long,
    language: String,
  ): MovieTranslation?

  suspend fun getAll(language: String): List<MovieTranslation>

  suspend fun insertSingle(translation: MovieTranslation)

  suspend fun deleteByLanguage(languages: List<String>)
}
