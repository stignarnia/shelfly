package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.data_local.database.model.Rating
import xyz.stignarnia.data_local.sources.RatingsLocalDataSource

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

  @Query("DELETE FROM ratings WHERE type == :type AND id_tmdb IN (:ids)")
  suspend fun deleteAllByType(
    type: String,
    ids: Set<Long>,
  )

  @Query("DELETE FROM ratings WHERE id_tmdb == :tmdbId AND type == :type")
  override suspend fun deleteByType(
    tmdbId: Long,
    type: String,
  )

  @Transaction
  override suspend fun replaceAll(
    ratings: List<Rating>,
    type: String,
  ) {
    deleteAllByType(type, ratings.map { it.idTmdb }.toSet())
    insert(ratings)
  }

  @Transaction
  override suspend fun replace(rating: Rating) {
    deleteByType(rating.idTmdb, rating.type)
    insert(listOf(rating))
  }
}
