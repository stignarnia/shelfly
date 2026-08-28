package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.ShowImage

interface ShowImagesLocalDataSource {
  suspend fun getByShowId(
    tmdbId: Long,
    type: String,
  ): ShowImage?

  suspend fun getByEpisodeId(
    tmdbId: Long,
    type: String,
  ): ShowImage?

  suspend fun insertShowImage(image: ShowImage)

  suspend fun insertEpisodeImage(image: ShowImage)

  suspend fun upsert(image: ShowImage)

  suspend fun deleteByShowId(
    id: Long,
    type: String,
  )

  suspend fun deleteByEpisodeId(
    id: Long,
    type: String,
  )

  suspend fun deleteAll()
}
