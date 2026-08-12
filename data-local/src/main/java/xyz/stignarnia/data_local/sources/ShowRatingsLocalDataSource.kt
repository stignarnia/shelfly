package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.ShowRatings

interface ShowRatingsLocalDataSource {

  suspend fun upsert(entity: ShowRatings)

  suspend fun getById(tmdbId: Long): ShowRatings?
}
