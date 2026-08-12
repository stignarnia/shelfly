package xyz.stignarnia.repository.movies

import xyz.stignarnia.common.ConfigVariant.COLLECTIONS_CACHE_DURATION
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.data_local.database.model.MovieCollectionItem
import xyz.stignarnia.data_local.sources.MovieCollectionsItemsLocalDataSource
import xyz.stignarnia.data_local.sources.MovieCollectionsLocalDataSource
import xyz.stignarnia.data_local.sources.MoviesLocalDataSource
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.repository.mappers.CollectionMapper
import xyz.stignarnia.repository.mappers.MovieMapper
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.MovieCollection
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieCollectionsRepository @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val remoteSource: TmdbRemoteDataSource,
  private val moviesLocalSource: MoviesLocalDataSource,
  private val movieCollectionsLocalSource: MovieCollectionsLocalDataSource,
  private val movieCollectionsItemsLocalSource: MovieCollectionsItemsLocalDataSource,
  private val collectionMapper: CollectionMapper,
  private val movieMapper: MovieMapper,
  private val transactions: TransactionsProvider,
) {

  suspend fun loadCollection(collectionId: IdTmdb) =
    withContext(dispatchers.IO) {
      movieCollectionsLocalSource.getById(collectionId.id)
    }

  suspend fun loadCollections(movieId: IdTmdb): Pair<List<MovieCollection>, Source> =
    withContext(dispatchers.IO) {
      val now = nowUtc()
      val localCollections = movieCollectionsLocalSource.getByMovieId(movieId.id)

      val localTimestamp = localCollections.firstOrNull()?.updatedAt
      localTimestamp?.let { timestamp ->
        if (now.toMillis() - timestamp.toMillis() < COLLECTIONS_CACHE_DURATION) {
          return@withContext Pair(
            localCollections.map { collectionMapper.fromEntity(it) },
            Source.LOCAL,
          )
        }
      }

      val remoteCollections = remoteSource.fetchMovieCollections(movieId.id)
      val collections = remoteCollections.map { collectionMapper.fromNetwork(it) }

      updateLocalCollections(collections, movieId, now)

      return@withContext Pair(
        collections,
        Source.REMOTE,
      )
    }

  suspend fun loadCollectionItems(collectionId: IdTmdb): List<Movie> =
    withContext(dispatchers.IO) {
      val now = nowUtc()
      val localItems = movieCollectionsItemsLocalSource.getById(collectionId.id)

      val localTimestamp = localItems.firstOrNull()?.updatedAt
      localTimestamp?.let { timestamp ->
        if (now.toMillis() - timestamp < COLLECTIONS_CACHE_DURATION) {
          return@withContext localItems.map { movieMapper.fromDatabase(it) }
        }
      }

      val remoteItems = remoteSource.fetchMovieCollectionItems(collectionId.id)
      val items = remoteItems.map { movieMapper.fromNetwork(it) }

      transactions.withTransaction {
        val entities = items.mapIndexed { index, movie ->
          MovieCollectionItem(
            rank = index,
            idTmdb = movie.tmdbId,
            idTmdbCollection = collectionId.id,
            createdAt = now,
            updatedAt = now,
          )
        }
        moviesLocalSource.upsert(items.map { movieMapper.toDatabase(it) })
        movieCollectionsItemsLocalSource.replace(collectionId.id, entities)

        // Fill up collection with other movies that belong in it.
        val collection = movieCollectionsLocalSource.getById(collectionId.id)
        collection?.let { coll ->
          val insertEntities = entities
            .filter { it.idTmdb != coll.idTmdbMovie }
            .map { coll.copy(id = 0, idTmdbMovie = it.idTmdb) }
          movieCollectionsLocalSource.insertAll(insertEntities)
        }
      }

      return@withContext items
    }

  private suspend fun updateLocalCollections(
    collections: List<MovieCollection>,
    movieId: IdTmdb,
    now: ZonedDateTime,
  ) {
    var entities = collections.map {
      collectionMapper.toEntity(
        movieId = movieId.id,
        input = it,
        updatedAt = now,
        createdAt = now,
      )
    }
    if (entities.isEmpty()) {
      entities = listOf(
        collectionMapper.toEntity(
          movieId.id,
          MovieCollection.EMPTY,
        ),
      )
    }
    movieCollectionsLocalSource.replaceByMovieId(movieId.id, entities)
  }

  enum class Source { LOCAL, REMOTE }
}
