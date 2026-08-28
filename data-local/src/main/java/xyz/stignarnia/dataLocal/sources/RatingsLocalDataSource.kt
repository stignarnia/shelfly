package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.Rating

interface RatingsLocalDataSource {
  suspend fun getAll(): List<Rating>

  suspend fun getAllByType(type: String): List<Rating>

  suspend fun getAllByType(
    idsTmdb: List<Long>,
    type: String,
  ): List<Rating>

  /** All season ratings recorded under [showTmdbId]. */
  suspend fun getSeasonRatings(showTmdbId: Long): List<Rating>

  suspend fun getSeasonRating(
    showTmdbId: Long,
    seasonNumber: Int,
  ): Rating?

  suspend fun getEpisodeRating(
    showTmdbId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
  ): Rating?

  suspend fun deleteByKey(
    tmdbId: Long,
    type: String,
    seasonNumber: Int,
    episodeNumber: Int,
  )

  suspend fun replace(rating: Rating)
}
