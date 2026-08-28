package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.dataLocal.database.model.Rating
import xyz.stignarnia.dataLocal.sources.RatingsLocalDataSource

@Dao
interface RatingsDao :
  BaseDao<Rating>,
  RatingsLocalDataSource {
  @Query("SELECT * FROM ratings ORDER BY rated_at DESC")
  override suspend fun getAll(): List<Rating>

  @Query("SELECT * FROM ratings WHERE type == :type ORDER BY rated_at DESC")
  override suspend fun getAllByType(type: String): List<Rating>

  @Query("SELECT * FROM ratings WHERE id_tmdb IN (:idsTmdb) AND type == :type ORDER BY rated_at DESC")
  override suspend fun getAllByType(
    idsTmdb: List<Long>,
    type: String,
  ): List<Rating>

  @Query("SELECT * FROM ratings WHERE id_tmdb == :showTmdbId AND type == 'season' ORDER BY season_number")
  override suspend fun getSeasonRatings(showTmdbId: Long): List<Rating>

  @Query(
    "SELECT * FROM ratings WHERE id_tmdb == :showTmdbId AND type == 'season' " +
      "AND season_number == :seasonNumber LIMIT 1",
  )
  override suspend fun getSeasonRating(
    showTmdbId: Long,
    seasonNumber: Int,
  ): Rating?

  @Query(
    "SELECT * FROM ratings WHERE id_tmdb == :showTmdbId AND type == 'episode' " +
      "AND season_number == :seasonNumber AND episode_number == :episodeNumber LIMIT 1",
  )
  override suspend fun getEpisodeRating(
    showTmdbId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
  ): Rating?

  @Query(
    "DELETE FROM ratings WHERE id_tmdb == :tmdbId AND type == :type " +
      "AND season_number == :seasonNumber AND episode_number == :episodeNumber",
  )
  override suspend fun deleteByKey(
    tmdbId: Long,
    type: String,
    seasonNumber: Int,
    episodeNumber: Int,
  )

  @Transaction
  override suspend fun replace(rating: Rating) {
    deleteByKey(rating.idTmdb, rating.type, rating.seasonNumber, rating.episodeNumber)
    insert(listOf(rating))
  }
}
