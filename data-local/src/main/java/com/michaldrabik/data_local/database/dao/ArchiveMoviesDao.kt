@file:Suppress("ktlint:standard:max-line-length")

package com.michaldrabik.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.michaldrabik.data_local.database.model.ArchiveMovie
import com.michaldrabik.data_local.database.model.Movie
import com.michaldrabik.data_local.sources.ArchiveMoviesLocalDataSource

@Dao
interface ArchiveMoviesDao : ArchiveMoviesLocalDataSource {

  @Query(
    "SELECT " +
      "movies.id_tmdb, " +
      "movies.id_tmdb, " +
      "movies.id_imdb, " +
      "movies.id_slug, " +
      "movies.title, " +
      "movies.year, " +
      "movies.overview, " +
      "movies.released, " +
      "movies.runtime, " +
      "movies.country, " +
      "movies.trailer, " +
      "movies.language, " +
      "movies.homepage, " +
      "movies.status, " +
      "movies.rating, " +
      "movies.votes, " +
      "movies.comment_count, " +
      "movies.genres, " +
      "movies_archive.updated_at, " +
      "movies_archive.created_at " +
      "FROM movies " +
      "INNER JOIN movies_archive USING(id_tmdb)",
  )
  override suspend fun getAll(): List<Movie>

  @Query(
    "SELECT " +
      "movies.id_tmdb, " +
      "movies.id_tmdb, " +
      "movies.id_imdb, " +
      "movies.id_slug, " +
      "movies.title, " +
      "movies.year, " +
      "movies.overview, " +
      "movies.released, " +
      "movies.runtime, " +
      "movies.country, " +
      "movies.trailer, " +
      "movies.language, " +
      "movies.homepage, " +
      "movies.status, " +
      "movies.rating, " +
      "movies.votes, " +
      "movies.comment_count, " +
      "movies.genres, " +
      "movies_archive.updated_at, " +
      "movies_archive.created_at " +
      "FROM movies " +
      "INNER JOIN movies_archive USING(id_tmdb) WHERE id_tmdb IN (:ids)",
  )
  override suspend fun getAll(ids: List<Long>): List<Movie>

  @Query("SELECT movies.id_tmdb FROM movies INNER JOIN movies_archive USING(id_tmdb)")
  override suspend fun getAllTraktIds(): List<Long>

  @Query("SELECT movies.* FROM movies INNER JOIN movies_archive USING(id_tmdb) WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): Movie?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insert(movie: ArchiveMovie)

  @Query("DELETE FROM movies_archive WHERE id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)
}
