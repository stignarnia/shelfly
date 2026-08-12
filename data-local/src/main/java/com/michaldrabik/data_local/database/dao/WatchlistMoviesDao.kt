package com.michaldrabik.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.michaldrabik.data_local.database.model.Movie
import com.michaldrabik.data_local.database.model.WatchlistMovie
import com.michaldrabik.data_local.sources.WatchlistMoviesLocalDataSource

@Dao
interface WatchlistMoviesDao : WatchlistMoviesLocalDataSource {

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
      "movies_see_later.updated_at, " +
      "movies_see_later.created_at " +
      "FROM movies " +
      "INNER JOIN movies_see_later USING(id_tmdb)",
  )
  override suspend fun getAll(): List<Movie>

  @Query("SELECT movies.id_tmdb FROM movies INNER JOIN movies_see_later USING(id_tmdb)")
  override suspend fun getAllTmdbIds(): List<Long>

  @Query(
    "SELECT movies.* FROM movies " +
      "INNER JOIN movies_see_later ON movies_see_later.id_tmdb == movies.id_tmdb WHERE movies.id_tmdb == :tmdbId",
  )
  override suspend fun getById(tmdbId: Long): Movie?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insert(movie: WatchlistMovie)

  @Query("DELETE FROM movies_see_later WHERE id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)

  @Query("SELECT EXISTS(SELECT 1 FROM movies_see_later WHERE id_tmdb = :tmdbId LIMIT 1);")
  override suspend fun checkExists(tmdbId: Long): Boolean
}
