package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.MovieRatings

interface MovieRatingsLocalDataSource {
  suspend fun upsert(entity: MovieRatings)

  suspend fun getById(tmdbId: Long): MovieRatings?
}
