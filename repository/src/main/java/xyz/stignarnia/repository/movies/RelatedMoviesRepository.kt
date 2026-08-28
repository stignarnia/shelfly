package xyz.stignarnia.repository.movies

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.RelatedMovie
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject

class RelatedMoviesRepository
  @Inject
  constructor(
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalDataSource,
    private val transactions: TransactionsProvider,
    private val mappers: Mappers,
  ) {
    suspend fun loadAll(movie: Movie): List<Movie> {
      val related = localSource.relatedMovies.getAllById(movie.ids.tmdb.id)
      val latest = related.maxByOrNull { it.updatedAt }

      if (latest != null && nowUtcMillis() - latest.updatedAt < Config.RELATED_CACHE_DURATION) {
        val relatedIds = related.map { it.idTmdb }
        return localSource.movies
          .getAll(relatedIds)
          .map { mappers.movie.fromDatabase(it) }
      }

      val remote =
        remoteSource.tmdb
          .fetchRelatedMovies(movie.ids.tmdb.id)
          .map { mappers.movie.fromNetwork(it) }

      cacheRelated(remote, movie.ids.tmdb)

      return remote
    }

    private suspend fun cacheRelated(
      movies: List<Movie>,
      movieId: IdTmdb,
    ) {
      transactions.withTransaction {
        val timestamp = nowUtcMillis()
        localSource.movies.upsert(movies.map { mappers.movie.toDatabase(it) })
        localSource.relatedMovies.deleteById(movieId.id)
        localSource.relatedMovies.insert(
          movies.map {
            RelatedMovie.fromTmdbId(it.ids.tmdb.id, movieId.id, timestamp)
          },
        )
      }
    }
  }
