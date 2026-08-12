package xyz.stignarnia.repository.shows.ratings

import xyz.stignarnia.common.ConfigVariant
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.Ratings
import xyz.stignarnia.ui_model.Show
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowsExternalRatingsRepository @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  suspend fun loadRatings(show: Show): Ratings {
    val localRatings = localSource.showRatings.getById(show.tmdbId)
    localRatings?.let {
      if (nowUtcMillis() - it.updatedAt < ConfigVariant.RATINGS_CACHE_DURATION) {
        return mappers.ratings.fromDatabase(it)
      }
    }

    val remoteRatings = remoteSource.omdb
      .fetchOmdbData(show.ids.imdb.id)
      .let { mappers.ratings.fromNetwork(it) }
      .copy(tmdb = Ratings.Value(String.format(Locale.ENGLISH, "%.1f", show.rating), false))

    val dbRatings = mappers.ratings.toShowDatabase(show.ids.tmdb, remoteRatings)
    localSource.showRatings.upsert(dbRatings)

    return remoteRatings
  }
}
