package xyz.stignarnia.repository.movies

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcZone
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.MyMovie
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.IdTmdb
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
