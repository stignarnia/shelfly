package xyz.stignarnia.dataRemote.omdb

import xyz.stignarnia.dataRemote.omdb.model.OmdbResult

/**
 * Fetch/post remote resources via OMDB API
 */
interface OmdbRemoteDataSource {
  suspend fun fetchOmdbData(imdbId: String): OmdbResult
}
