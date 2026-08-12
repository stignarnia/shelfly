package xyz.stignarnia.repository.shows

import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.StreamingsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.StreamingService
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowStreamingsRepository @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) : StreamingsRepository() {

  suspend fun getLocalStreamings(
    show: Show,
    countryCode: String,
  ): Pair<List<StreamingService>, ZonedDateTime?> {
    val localItems = localSource.showStreamings.getById(show.tmdbId)
    val mappedItems = mappers.streamings.fromDatabaseShow(localItems, show.title, countryCode)

    val processedItems = processItems(mappedItems, countryCode)
    val date = localItems.firstOrNull()?.createdAt
    return Pair(processedItems, date)
  }

  suspend fun loadRemoteStreamings(
    show: Show,
    countryCode: String,
  ): List<StreamingService> {
    val remoteItems = remoteSource.tmdb.fetchShowWatchProviders(show.ids.tmdb.id, countryCode) ?: return emptyList()

    val entities = mappers.streamings.toDatabaseShow(show.ids, remoteItems)
    localSource.showStreamings.replace(show.tmdbId, entities)

    return processItems(remoteItems, show.title, countryCode)
  }

  suspend fun deleteCache() = localSource.showStreamings.deleteAll()
}
