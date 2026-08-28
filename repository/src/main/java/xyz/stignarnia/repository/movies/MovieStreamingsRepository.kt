package xyz.stignarnia.repository.movies

import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.StreamingsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.StreamingService
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieStreamingsRepository
  @Inject
  constructor(
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
  ) : StreamingsRepository() {
    suspend fun getLocalStreamings(
      movie: Movie,
      countryCode: String,
    ): Pair<List<StreamingService>, ZonedDateTime?> {
      val localItems = localSource.movieStreamings.getById(movie.tmdbId)
      val mappedItems = mappers.streamings.fromDatabaseMovie(localItems, movie.title, countryCode)

      val processedItems = processItems(mappedItems, countryCode)
      val date = localItems.firstOrNull()?.createdAt
      return Pair(processedItems, date)
    }

    suspend fun loadRemoteStreamings(
      movie: Movie,
      countryCode: String,
    ): List<StreamingService> {
      val remoteItems = remoteSource.tmdb.fetchMovieWatchProviders(movie.ids.tmdb.id, countryCode) ?: return emptyList()

      val entities = mappers.streamings.toDatabaseMovie(movie.ids, remoteItems)
      localSource.movieStreamings.replace(movie.tmdbId, entities)

      return processItems(remoteItems, movie.title, countryCode)
    }

    suspend fun deleteCache() = localSource.movieStreamings.deleteAll()
  }
