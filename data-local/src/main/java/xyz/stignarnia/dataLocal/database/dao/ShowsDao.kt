package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.dataLocal.database.model.Show
import xyz.stignarnia.dataLocal.database.model.ShowSearch
import xyz.stignarnia.dataLocal.sources.ShowsLocalDataSource

@Dao
interface ShowsDao :
  BaseDao<Show>,
  ShowsLocalDataSource {
  @Query("SELECT * FROM shows")
  override suspend fun getAll(): List<Show>

  @Query("SELECT * FROM shows WHERE id_tmdb IN (:ids)")
  override suspend fun getAll(ids: List<Long>): List<Show>

  @Query("SELECT id_tmdb AS key_id, id_tmdb AS value_id FROM shows WHERE id_tmdb IN (:tmdbIds)")
  override suspend fun getAllTmdbIds(
    tmdbIds: List<Long>,
  ): Map<
    @MapColumn(columnName = "key_id")
    Long,
    @MapColumn(columnName = "value_id")
    Long,
  >

  @Query("SELECT shows.id_tmdb, shows.title FROM shows")
  override suspend fun getAllForSearch(): List<ShowSearch>

  @Transaction
  override suspend fun getAllChunked(ids: List<Long>): List<Show> =
    ids
      .chunked(500)
      .fold(mutableListOf()) { acc, chunk ->
        acc += getAll(chunk)
        acc
      }

  @Query("SELECT * FROM shows WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): Show?

  @Query("SELECT * FROM shows WHERE id_tmdb == :tmdbId")
  override suspend fun getByTmdbId(tmdbId: Long): Show?

  @Query("SELECT * FROM shows WHERE id_slug == :slug")
  override suspend fun getBySlug(slug: String): Show?

  @Query("SELECT * FROM shows WHERE id_imdb == :imdbId")
  override suspend fun getById(imdbId: String): Show?

  @Query("DELETE FROM shows where id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)

  @Transaction
  override suspend fun upsert(shows: List<Show>) {
    val result = insert(shows)

    val updateList = mutableListOf<Show>()
    result.forEachIndexed { index, id ->
      if (id == -1L) updateList.add(shows[index])
    }

    if (updateList.isNotEmpty()) update(updateList)
  }
}
