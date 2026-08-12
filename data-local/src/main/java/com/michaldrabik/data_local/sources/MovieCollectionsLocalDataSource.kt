package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.MovieCollection

interface MovieCollectionsLocalDataSource {

  suspend fun getById(tmdbId: Long): MovieCollection?

  suspend fun getByMovieId(movieTraktId: Long): List<MovieCollection>

  suspend fun replaceByMovieId(
    movieTraktId: Long,
    entities: List<MovieCollection>,
  )

  suspend fun insertAll(items: List<MovieCollection>)
}
