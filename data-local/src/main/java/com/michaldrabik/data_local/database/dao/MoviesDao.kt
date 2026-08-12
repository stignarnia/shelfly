package com.michaldrabik.data_local.database.dao

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import com.michaldrabik.data_local.database.model.Movie
import com.michaldrabik.data_local.database.model.MovieSearch
import com.michaldrabik.data_local.sources.MoviesLocalDataSource

@Dao
interface MoviesDao :
  BaseDao<Movie>,
  MoviesLocalDataSource {

  @Query("SELECT * FROM movies")
  override suspend fun getAll(): List<Movie>

  @Query("SELECT * FROM movies WHERE id_tmdb IN (:ids)")
  override suspend fun getAll(ids: List<Long>): List<Movie>

  @Query("SELECT id_tmdb, id_tmdb FROM movies WHERE id_tmdb IN (:tmdbIds)")
  override suspend fun getAllTmdbIds(
    tmdbIds: List<Long>,
  ): Map<@MapColumn(columnName = "id_tmdb") Long, @MapColumn(columnName = "id_tmdb") Long>

  @Query("SELECT movies.id_tmdb, movies.title FROM movies")
  override suspend fun getAllForSearch(): List<MovieSearch>

  @Transaction
  override suspend fun getAllChunked(ids: List<Long>): List<Movie> =
    ids
      .chunked(500)
      .fold(
        mutableListOf(),
      ) { acc, chunk ->
        acc += getAll(chunk)
        acc
      }

  @Query("SELECT * FROM movies WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): Movie?

  @Query("SELECT * FROM movies WHERE id_tmdb == :tmdbId")
  override suspend fun getByTmdbId(tmdbId: Long): Movie?

  @Query("SELECT * FROM movies WHERE id_slug == :slug")
  override suspend fun getBySlug(slug: String): Movie?

  @Query("SELECT * FROM movies WHERE id_imdb == :imdbId")
  override suspend fun getById(imdbId: String): Movie?

  @Query("DELETE FROM movies where id_tmdb == :tmdbId")
  override suspend fun deleteById(tmdbId: Long)

  @Transaction
  override suspend fun upsert(movies: List<Movie>) {
    val result = insert(movies)

    val updateList = mutableListOf<Movie>()
    result.forEachIndexed { index, id ->
      if (id == -1L) {
        updateList.add(movies[index])
      }
    }
    if (updateList.isNotEmpty()) {
      update(updateList)
    }
  }
}
