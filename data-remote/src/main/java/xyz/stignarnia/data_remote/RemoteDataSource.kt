package xyz.stignarnia.data_remote

import xyz.stignarnia.data_remote.omdb.OmdbRemoteDataSource
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides external data sources access points.
 */
interface RemoteDataSource {
  val tmdb: TmdbRemoteDataSource
  val omdb: OmdbRemoteDataSource
}

@Singleton
internal class MainRemoteDataSource @Inject constructor(
  override val tmdb: TmdbRemoteDataSource,
  override val omdb: OmdbRemoteDataSource,
) : RemoteDataSource
