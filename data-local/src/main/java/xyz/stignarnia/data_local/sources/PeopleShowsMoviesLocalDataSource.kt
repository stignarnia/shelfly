package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.PersonShowMovie

interface PeopleShowsMoviesLocalDataSource {

  suspend fun getTimestampForShow(showTmdbId: Long): Long?

  suspend fun getTimestampForMovie(movieTmdbId: Long): Long?

  suspend fun deleteAllForShow(showTmdbId: Long)

  suspend fun deleteAllForMovie(movieTmdbId: Long)

  suspend fun insertForShow(
    people: List<PersonShowMovie>,
    showTmdbId: Long,
  )

  suspend fun insertForMovie(
    people: List<PersonShowMovie>,
    movieTmdbId: Long,
  )
}
