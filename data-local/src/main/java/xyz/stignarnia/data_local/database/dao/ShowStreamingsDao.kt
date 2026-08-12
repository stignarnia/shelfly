package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.data_local.database.model.ShowStreaming
import xyz.stignarnia.data_local.sources.ShowStreamingsLocalDataSource

@Dao
interface ShowStreamingsDao :
  BaseDao<ShowStreaming>,
  ShowStreamingsLocalDataSource {

  @Transaction
  override suspend fun replace(
    tmdbId: Long,
    entities: List<ShowStreaming>,
  ) {
    deleteById(tmdbId)
    insert(entities)
  }

  @Query("SELECT * FROM shows_streamings WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): List<ShowStreaming>

  @Query("DELETE FROM shows_streamings WHERE id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)

  @Query("DELETE FROM shows_streamings")
  override suspend fun deleteAll()
}
