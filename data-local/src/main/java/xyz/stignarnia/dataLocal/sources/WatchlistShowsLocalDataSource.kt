package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.Show
import xyz.stignarnia.dataLocal.database.model.WatchlistShow

interface WatchlistShowsLocalDataSource {
  suspend fun getAll(): List<Show>

  suspend fun getAllTmdbIds(): List<Long>

  suspend fun getById(tmdbId: Long): Show?

  suspend fun insert(show: WatchlistShow)

  suspend fun deleteById(tmdbId: Long)

  suspend fun checkExists(tmdbId: Long): Boolean
}
