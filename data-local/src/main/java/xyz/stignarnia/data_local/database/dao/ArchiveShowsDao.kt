@file:Suppress("ktlint:standard:max-line-length")

package xyz.stignarnia.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.data_local.database.model.ArchiveShow
import xyz.stignarnia.data_local.database.model.Show
import xyz.stignarnia.data_local.sources.ArchiveShowsLocalDataSource

@Dao
interface ArchiveShowsDao : ArchiveShowsLocalDataSource {

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
      "shows.network_logo_path, " +
      "shows.country, " +
      "shows.trailer, " +
      "shows.homepage, " +
      "shows.status, " +
      "shows.rating, " +
      "shows.votes, " +
      "shows.comment_count, " +
      "shows.genres, " +
      "shows.aired_episodes, " +
      "shows_archive.updated_at, " +
      "shows_archive.created_at " +
      "FROM shows " +
      "INNER JOIN shows_archive USING(id_tmdb)",
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
      "shows.network_logo_path, " +
      "shows.country, " +
      "shows.trailer, " +
      "shows.homepage, " +
      "shows.status, " +
      "shows.rating, " +
      "shows.votes, " +
      "shows.comment_count, " +
      "shows.genres, " +
      "shows.aired_episodes, " +
      "shows_archive.updated_at, " +
      "shows_archive.created_at " +
      "FROM shows " +
      "INNER JOIN shows_archive USING(id_tmdb) WHERE id_tmdb IN (:ids)",
  )
  override suspend fun getAll(ids: List<Long>): List<Show>

  @Query("SELECT shows.id_tmdb FROM shows INNER JOIN shows_archive USING(id_tmdb)")
  override suspend fun getAllTmdbIds(): List<Long>

  @Query("SELECT shows.* FROM shows INNER JOIN shows_archive USING(id_tmdb) WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): Show?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insert(show: ArchiveShow)

  @Query("DELETE FROM shows_archive WHERE id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)
}
