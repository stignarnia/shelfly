package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.MovieCollection

interface MovieCollectionsLocalDataSource {

  suspend fun getById(tmdbId: Long): MovieCollection?

  suspend fun getByMovieId(movieTmdbId: Long): List<MovieCollection>

  suspend fun replaceByMovieId(
    movieTmdbId: Long,
    entities: List<MovieCollection>,
  )

  suspend fun insertAll(items: List<MovieCollection>)
}
