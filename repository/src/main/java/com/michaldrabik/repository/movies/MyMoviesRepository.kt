package com.michaldrabik.repository.movies

import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.common.extensions.toMillis
import com.michaldrabik.common.extensions.toUtcZone
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.MyMovie
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.IdTmdb
import java.time.ZonedDateTime
import javax.inject.Inject

class MyMoviesRepository @Inject constructor(
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun load(id: IdTmdb) =
    localSource.myMovies.getById(id.id)?.let {
      mappers.movie.fromDatabase(it)
    }

  suspend fun loadAll() =
    localSource.myMovies
      .getAll()
      .map { mappers.movie.fromDatabase(it) }

  suspend fun loadAll(ids: List<IdTmdb>) =
    localSource.myMovies
      .getAll(ids.map { it.id })
      .map { mappers.movie.fromDatabase(it) }

  suspend fun loadAllRecent(amount: Int) =
    localSource.myMovies
      .getAllRecent(amount)
      .map { mappers.movie.fromDatabase(it) }

  suspend fun loadAllIds() = localSource.myMovies.getAllTmdbIds()

  suspend fun insert(
    id: IdTmdb,
    customDate: ZonedDateTime?,
  ) {
    val movie = MyMovie.fromTmdbId(
      tmdbId = id.id,
      timestamp = customDate?.toUtcZone()?.toMillis() ?: nowUtcMillis(),
    )
    transactions.withTransaction {
      with(localSource) {
        myMovies.insert(listOf(movie))
        watchlistMovies.deleteById(id.id)
        archiveMovies.deleteById(id.id)
      }
    }
  }

  suspend fun delete(id: IdTmdb) = localSource.myMovies.deleteById(id.id)

  suspend fun exists(id: IdTmdb) = localSource.myMovies.checkExists(id.id)
}
