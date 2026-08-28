
package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.dataLocal.database.model.Movie
import xyz.stignarnia.dataLocal.database.model.PersonCredits
import xyz.stignarnia.dataLocal.database.model.Show
import xyz.stignarnia.dataLocal.sources.PeopleCreditsLocalDataSource

@Dao
interface PeopleCreditsDao :
  BaseDao<PersonCredits>,
  PeopleCreditsLocalDataSource {
  @Query(
    """
    SELECT
    shows.id_tmdb,
    shows.id_tvdb,
    shows.id_tmdb,
    shows.id_imdb,
    shows.id_slug,
    shows.id_tvrage,
    shows.title,
    shows.year,
    shows.overview,
    shows.first_aired,
    shows.runtime,
    shows.airtime_day,
    shows.airtime_time,
    shows.airtime_timezone,
    shows.certification,
    shows.network,
    shows.network_logo_path,
    shows.country,
    shows.trailer,
    shows.homepage,
    shows.status,
    shows.rating,
    shows.votes,
    shows.comment_count,
    shows.genres,
    shows.aired_episodes,
    people_credits.created_at AS created_at,
    people_credits.updated_at AS updated_at
    FROM shows
    INNER JOIN people_credits ON people_credits.id_tmdb_show = shows.id_tmdb
    WHERE people_credits.id_tmdb_person = :personTmdbId
    """,
  )
  override suspend fun getAllShowsForPerson(personTmdbId: Long): List<Show>

  @Query(
    """
    SELECT
    movies.id_tmdb,
    movies.id_tmdb,
    movies.id_imdb,
    movies.id_slug,
    movies.title,
    movies.year,
    movies.overview,
    movies.released,
    movies.runtime,
    movies.country,
    movies.trailer,
    movies.language,
    movies.homepage,
    movies.status,
    movies.rating,
    movies.votes,
    movies.comment_count,
    movies.genres,
    people_credits.updated_at AS updated_at,
    people_credits.created_at AS created_at
    FROM movies
    INNER JOIN people_credits ON people_credits.id_tmdb_movie = movies.id_tmdb
    WHERE people_credits.id_tmdb_person = :personTmdbId
    """,
  )
  override suspend fun getAllMoviesForPerson(personTmdbId: Long): List<Movie>

  @Query("SELECT updated_at FROM people_credits WHERE id_tmdb_person = :personTmdbId LIMIT 1")
  override suspend fun getTimestampForPerson(personTmdbId: Long): Long?

  @Query("DELETE FROM people_credits WHERE id_tmdb_person == :personTmdbId")
  override suspend fun deleteAllForPerson(personTmdbId: Long)

  @Transaction
  override suspend fun insertSingle(
    personTmdbId: Long,
    credits: List<PersonCredits>,
  ) {
    deleteAllForPerson(personTmdbId)
    insert(credits)
  }
}
