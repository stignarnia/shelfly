package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.MyShow
import xyz.stignarnia.dataLocal.database.model.Show

interface MyShowsLocalDataSource {
  suspend fun getAll(): List<Show>

  suspend fun getAll(ids: List<Long>): List<Show>

  suspend fun getAllRecent(limit: Int): List<Show>

  suspend fun getAllTmdbIds(): List<Long>

  suspend fun getById(tmdbId: Long): Show?

  suspend fun updateWatchedAt(
    tmdbId: Long,
    watchedAt: Long,
  )

  suspend fun insert(shows: List<MyShow>)

  suspend fun deleteById(tmdbId: Long)

  suspend fun checkExists(tmdbId: Long): Boolean
}
