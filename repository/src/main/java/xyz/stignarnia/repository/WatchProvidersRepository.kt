package xyz.stignarnia.repository

import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.uiModel.StreamingProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The streaming services available in one region, which is what the discover filter offers instead of a fixed list of channel names.
 *
 * TMDB publishes a separate directory per media type, so a service that only carries films never appears among the show filters and the other way round.
 */
@Singleton
class WatchProvidersRepository
  @Inject
  constructor(
    private val remoteSource: RemoteDataSource,
  ) {
    // The directory changes on TMDB's schedule, not the user's, so it is held for the process lifetime.
    // Keyed by region as well as type: the region is a setting the user can change without restarting.
    private val cache = ConcurrentHashMap<Pair<Boolean, String>, List<StreamingProvider>>()

    suspend fun loadProviders(
      isMovie: Boolean,
      countryCode: String,
    ): List<StreamingProvider> {
      val key = isMovie to countryCode.lowercase()
      cache[key]?.let { return it }

      val providers =
        remoteSource.tmdb
          .fetchWatchProviders(isMovie, countryCode)
          .map {
            StreamingProvider(
              id = it.provider_id,
              name = it.provider_name,
              logoPath = it.logo_path ?: "",
            )
          }

      cache[key] = providers
      return providers
    }
  }
