package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.Person

interface PeopleLocalDataSource {
  suspend fun upsert(people: List<Person>)

  suspend fun getById(tmdbId: Long): Person?

  suspend fun getAllForShow(showTmdbId: Long): List<Person>

  suspend fun getAllForMovie(movieTmdbId: Long): List<Person>

  suspend fun getAll(): List<Person>

  suspend fun deleteTranslations()
}
