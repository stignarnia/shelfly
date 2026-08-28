package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.ShowRatings

interface ShowRatingsLocalDataSource {
  suspend fun upsert(entity: ShowRatings)

  suspend fun getById(tmdbId: Long): ShowRatings?
}
