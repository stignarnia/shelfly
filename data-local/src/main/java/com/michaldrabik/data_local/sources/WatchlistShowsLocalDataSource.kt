package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.Show
import com.michaldrabik.data_local.database.model.WatchlistShow

interface WatchlistShowsLocalDataSource {

  suspend fun getAll(): List<Show>

  suspend fun getAllTraktIds(): List<Long>

  suspend fun getById(tmdbId: Long): Show?

  suspend fun insert(show: WatchlistShow)

  suspend fun deleteById(tmdbId: Long)

  suspend fun checkExists(tmdbId: Long): Boolean
}
