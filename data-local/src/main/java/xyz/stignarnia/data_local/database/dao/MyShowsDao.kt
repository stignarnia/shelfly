@file:Suppress("ktlint:standard:max-line-length")

package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.data_local.database.model.MyShow
import xyz.stignarnia.data_local.database.model.Show
import xyz.stignarnia.data_local.sources.MyShowsLocalDataSource

@Dao
interface MyShowsDao : MyShowsLocalDataSource {

  @Query(
    "SELECT " +
      "shows.id_tmdb, " +
      "shows.id_tvdb, " +
      "shows.id_tmdb, " +
      "shows.id_imdb, " +
      "shows.id_slug, " +
      "shows.id_tvrage, " +
      "shows.title, " +
      "shows.year, " +
      "shows.overview, " +
      "shows.first_aired, " +
      "shows.runtime, " +
      "shows.airtime_day, " +
      "shows.airtime_time, " +
      "shows.airtime_timezone, " +
      "shows.certification, " +
      "shows.network, " +
      "shows.country, " +
      "shows.trailer, " +
      "shows.homepage, " +
      "shows.status, " +
      "shows.rating, " +
      "shows.votes, " +
      "shows.comment_count, " +
      "shows.genres, " +
      "shows.aired_episodes, " +
      "shows_my_shows.last_watched_at AS updated_at, " +
      "shows_my_shows.created_at " +
      "FROM shows " +
      "INNER JOIN shows_my_shows USING(id_tmdb)",
  )
  override suspend fun getAll(): List<Show>

  @Query(
    "SELECT " +
      "shows.id_tmdb, " +
      "shows.id_tvdb, " +
      "shows.id_tmdb, " +
      "shows.id_imdb, " +
      "shows.id_slug, " +
      "shows.id_tvrage, " +
      "shows.title, " +
      "shows.year, " +
      "shows.overview, " +
      "shows.first_aired, " +
      "shows.runtime, " +
      "shows.airtime_day, " +
      "shows.airtime_time, " +
      "shows.airtime_timezone, " +
      "shows.certification, " +
      "shows.network, " +
      "shows.country, " +
      "shows.trailer, " +
      "shows.homepage, " +
      "shows.status, " +
      "shows.rating, " +
      "shows.votes, " +
      "shows.comment_count, " +
      "shows.genres, " +
      "shows.aired_episodes, " +
      "shows_my_shows.last_watched_at AS updated_at, " +
      "shows_my_shows.created_at " +
      "FROM shows " +
      "INNER JOIN shows_my_shows USING(id_tmdb) WHERE id_tmdb IN (:ids)",
  )
  override suspend fun getAll(ids: List<Long>): List<Show>

  @Query(
    "SELECT shows.* FROM shows INNER JOIN shows_my_shows USING(id_tmdb) ORDER BY shows_my_shows.created_at DESC LIMIT :limit",
  )
  override suspend fun getAllRecent(limit: Int): List<Show>

  @Query("SELECT shows.id_tmdb FROM shows INNER JOIN shows_my_shows USING(id_tmdb)")
  override suspend fun getAllTmdbIds(): List<Long>

  @Query("SELECT shows.* FROM shows INNER JOIN shows_my_shows USING(id_tmdb) WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): Show?

  @Query("UPDATE shows_my_shows SET last_watched_at = :watchedAt WHERE id_tmdb == :tmdbId")
  override suspend fun updateWatchedAt(
    tmdbId: Long,
    watchedAt: Long,
  )

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insert(shows: List<MyShow>)

  @Query("DELETE FROM shows_my_shows WHERE id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)

  @Query("SELECT EXISTS(SELECT 1 FROM shows_my_shows WHERE id_tmdb = :tmdbId LIMIT 1);")
  override suspend fun checkExists(tmdbId: Long): Boolean
}
