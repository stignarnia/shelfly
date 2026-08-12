package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.Movie
import xyz.stignarnia.data_local.database.model.PersonCredits
import xyz.stignarnia.data_local.database.model.Show

interface PeopleCreditsLocalDataSource {

  suspend fun getAllShowsForPerson(personTmdbId: Long): List<Show>

  suspend fun getAllMoviesForPerson(personTmdbId: Long): List<Movie>

  suspend fun getTimestampForPerson(personTmdbId: Long): Long?

  suspend fun deleteAllForPerson(personTmdbId: Long)

  suspend fun insertSingle(
    personTmdbId: Long,
    credits: List<PersonCredits>,
  )
}
