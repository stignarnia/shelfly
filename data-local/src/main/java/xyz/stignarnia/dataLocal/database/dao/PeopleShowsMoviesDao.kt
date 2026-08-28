package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.dataLocal.database.model.PersonShowMovie
import xyz.stignarnia.dataLocal.sources.PeopleShowsMoviesLocalDataSource

@Dao
interface PeopleShowsMoviesDao :
  BaseDao<PersonShowMovie>,
  PeopleShowsMoviesLocalDataSource {
  @Query("SELECT updated_at FROM people_shows_movies WHERE id_tmdb_show == :showTmdbId LIMIT 1")
  override suspend fun getTimestampForShow(showTmdbId: Long): Long?

  @Query("SELECT updated_at FROM people_shows_movies WHERE id_tmdb_movie == :movieTmdbId LIMIT 1")
  override suspend fun getTimestampForMovie(movieTmdbId: Long): Long?

  @Query("DELETE FROM people_shows_movies WHERE id_tmdb_show == :showTmdbId")
  override suspend fun deleteAllForShow(showTmdbId: Long)

  @Query("DELETE FROM people_shows_movies WHERE id_tmdb_movie == :movieTmdbId")
  override suspend fun deleteAllForMovie(movieTmdbId: Long)

  @Transaction
  override suspend fun insertForShow(
    people: List<PersonShowMovie>,
    showTmdbId: Long,
  ) {
    deleteAllForShow(showTmdbId)
    insert(people)
  }

  @Transaction
  override suspend fun insertForMovie(
    people: List<PersonShowMovie>,
    movieTmdbId: Long,
  ) {
    deleteAllForMovie(movieTmdbId)
    insert(people)
  }
}
