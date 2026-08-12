package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.EpisodeTranslation

interface EpisodeTranslationsLocalDataSource {

  suspend fun getById(
    tmdbEpisodeId: Long,
    tmdbShowId: Long,
    language: String,
  ): EpisodeTranslation?

  suspend fun getByIds(
    tmdbEpisodeIds: List<Long>,
    tmdbShowId: Long,
    language: String,
  ): List<EpisodeTranslation>

  suspend fun insertSingle(translation: EpisodeTranslation)

  suspend fun deleteByLanguage(languages: List<String>)
}
