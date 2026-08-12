package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.MovieRatings

interface MovieRatingsLocalDataSource {

  suspend fun upsert(entity: MovieRatings)

  suspend fun getById(tmdbId: Long): MovieRatings?
}
