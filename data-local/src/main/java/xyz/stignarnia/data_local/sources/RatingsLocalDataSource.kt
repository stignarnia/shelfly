package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.Rating

interface RatingsLocalDataSource {

  suspend fun getAll(): List<Rating>

  suspend fun getAllByType(type: String): List<Rating>

  suspend fun getAllByType(
    idsTmdb: List<Long>,
    type: String,
  ): List<Rating>

  suspend fun deleteByType(
    tmdbId: Long,
    type: String,
  )

  suspend fun replaceAll(
    ratings: List<Rating>,
    type: String,
  )

  suspend fun replace(rating: Rating)
}
