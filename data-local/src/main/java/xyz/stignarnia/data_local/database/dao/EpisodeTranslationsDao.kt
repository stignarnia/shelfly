package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.data_local.database.model.EpisodeTranslation
import xyz.stignarnia.data_local.sources.EpisodeTranslationsLocalDataSource

@Dao
interface EpisodeTranslationsDao :
  BaseDao<EpisodeTranslation>,
  EpisodeTranslationsLocalDataSource {

  @Query(
    "SELECT * FROM episodes_translations " +
      "WHERE id_tmdb == :tmdbEpisodeId AND id_tmdb_show == :tmdbShowId AND language == :language",
  )
  override suspend fun getById(
    tmdbEpisodeId: Long,
    tmdbShowId: Long,
    language: String,
  ): EpisodeTranslation?

  @Query(
    "SELECT * FROM episodes_translations " +
      "WHERE id_tmdb IN (:tmdbEpisodeIds) AND id_tmdb_show == :tmdbShowId AND language == :language",
  )
  override suspend fun getByIds(
    tmdbEpisodeIds: List<Long>,
    tmdbShowId: Long,
    language: String,
  ): List<EpisodeTranslation>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insertSingle(translation: EpisodeTranslation)

  @Query("DELETE FROM episodes_translations WHERE language IN (:languages)")
  override suspend fun deleteByLanguage(languages: List<String>)
}
