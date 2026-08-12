package com.michaldrabik.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.michaldrabik.data_local.database.model.Movie
import com.michaldrabik.data_local.database.model.MyMovie
import com.michaldrabik.data_local.sources.MyMoviesLocalDataSource

@Dao
interface MyMoviesDao : MyMoviesLocalDataSource {

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
      "movies_my_movies.updated_at, " +
      "movies_my_movies.created_at " +
      "FROM movies " +
      "INNER JOIN movies_my_movies USING(id_tmdb)",
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
      "movies.created_at, " +
      "movies_my_movies.updated_at " +
      "FROM movies " +
      "INNER JOIN movies_my_movies USING(id_tmdb) WHERE id_tmdb IN (:ids)",
  )
  override suspend fun getAll(ids: List<Long>): List<Movie>

  @Query(
    "SELECT movies.* FROM movies " +
      "INNER JOIN movies_my_movies USING(id_tmdb) ORDER BY movies_my_movies.updated_at DESC LIMIT :limit",
  )
  override suspend fun getAllRecent(limit: Int): List<Movie>

  @Query("SELECT movies.id_tmdb FROM movies INNER JOIN movies_my_movies USING(id_tmdb)")
  override suspend fun getAllTraktIds(): List<Long>

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
      "movies.created_at, " +
      "movies_my_movies.updated_at " +
      "FROM movies " +
      "INNER JOIN movies_my_movies USING(id_tmdb) WHERE id_tmdb == :tmdbId",
  )
  override suspend fun getById(tmdbId: Long): Movie?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insert(movies: List<MyMovie>)

  @Query("DELETE FROM movies_my_movies WHERE id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)

  @Query("SELECT EXISTS(SELECT 1 FROM movies_my_movies WHERE id_tmdb = :tmdbId LIMIT 1);")
  override suspend fun checkExists(tmdbId: Long): Boolean
}
