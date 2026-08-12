package xyz.stignarnia.data_remote.omdb.api

import xyz.stignarnia.data_remote.omdb.OmdbRemoteDataSource

internal class OmdbApi(
  private val service: OmdbService,
) : OmdbRemoteDataSource {

  override suspend fun fetchOmdbData(imdbId: String) = service.fetchData(imdbId)
}
