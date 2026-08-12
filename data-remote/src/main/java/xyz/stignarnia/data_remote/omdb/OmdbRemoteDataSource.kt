package xyz.stignarnia.data_remote.omdb

import xyz.stignarnia.data_remote.omdb.model.OmdbResult

/**
 * Fetch/post remote resources via OMDB API
 */
interface OmdbRemoteDataSource {
  suspend fun fetchOmdbData(imdbId: String): OmdbResult
}
