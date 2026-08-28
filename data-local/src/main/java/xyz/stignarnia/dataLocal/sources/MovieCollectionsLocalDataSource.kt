package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.MovieCollection

interface MovieCollectionsLocalDataSource {
  suspend fun getById(tmdbId: Long): MovieCollection?

  suspend fun getByMovieId(movieTmdbId: Long): List<MovieCollection>

  suspend fun replaceByMovieId(
    movieTmdbId: Long,
    entities: List<MovieCollection>,
  )

  suspend fun insertAll(items: List<MovieCollection>)
}
