package xyz.stignarnia.dataRemote.omdb.api

import xyz.stignarnia.dataRemote.omdb.OmdbRemoteDataSource

internal class OmdbApi(
  private val service: OmdbService,
) : OmdbRemoteDataSource {
  override suspend fun fetchOmdbData(imdbId: String) = service.fetchData(imdbId)
}
