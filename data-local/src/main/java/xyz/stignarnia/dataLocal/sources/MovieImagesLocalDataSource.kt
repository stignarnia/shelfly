package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.MovieImage

interface MovieImagesLocalDataSource {
  suspend fun getByMovieId(
    tmdbId: Long,
    type: String,
  ): MovieImage?

  suspend fun insertMovieImage(image: MovieImage)

  suspend fun upsert(image: MovieImage)

  suspend fun deleteByMovieId(
    id: Long,
    type: String,
  )

  suspend fun deleteAll()
}
