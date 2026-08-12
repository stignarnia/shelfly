@file:Suppress("ktlint")

package com.michaldrabik.data_local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.michaldrabik.data_local.database.model.Person
import com.michaldrabik.data_local.sources.PeopleLocalDataSource

@Dao
interface PeopleDao : BaseDao<Person>, PeopleLocalDataSource {

  @Transaction
  override suspend fun upsert(people: List<Person>) {
    val result = insert(people)
    val updateList = mutableListOf<Person>()
    result.forEachIndexed { index, id ->
      if (id == -1L) {
        updateList.add(people[index])
      }
    }
    if (updateList.isNotEmpty()) update(updateList)
  }

  @Query("SELECT * FROM people WHERE id_tmdb = :tmdbId")
  override suspend fun getById(tmdbId: Long): Person?

  @Query(
    """
    SELECT
    people.id_tmdb,
    people.id_tmdb,
    people.id_imdb,
    people.name,
    people.biography,
    people.biography_translation,
    people.birthday,
    people.birthplace,
    people.deathday,
    people.image_path,
    people.homepage,
    people.created_at,
    people.updated_at,
    people.details_updated_at,
    people_shows_movies.department AS department,
    people_shows_movies.character AS character,
    people_shows_movies.job AS job,
    people_shows_movies.episodes_count AS episodes_count
    FROM people
    INNER JOIN people_shows_movies ON people_shows_movies.id_tmdb_person = people.id_tmdb
    WHERE people_shows_movies.id_tmdb_show = :showTmdbId
    """
  )
  override suspend fun getAllForShow(showTmdbId: Long): List<Person>

  @Query(
    """
    SELECT
    people.id_tmdb,
    people.id_tmdb,
    people.id_imdb,
    people.name,
    people.biography,
    people.biography_translation,
    people.birthday,
    people.birthplace,
    people.deathday,
    people.image_path,
    people.homepage,
    people.created_at,
    people.updated_at,
    people.details_updated_at,
    people_shows_movies.department AS department,
    people_shows_movies.character AS character,
    people_shows_movies.job AS job,
    people_shows_movies.episodes_count AS episodes_count
    FROM people
    INNER JOIN people_shows_movies ON people_shows_movies.id_tmdb_person = people.id_tmdb
    WHERE people_shows_movies.id_tmdb_movie = :movieTmdbId
    """
  )
  override suspend fun getAllForMovie(movieTmdbId: Long): List<Person>

  @Query("SELECT * FROM people")
  override suspend fun getAll(): List<Person>

  @Query("UPDATE people SET biography_translation = NULL, details_updated_at = NULL")
  override suspend fun deleteTranslations()
}
