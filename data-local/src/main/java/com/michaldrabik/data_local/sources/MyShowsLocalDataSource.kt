package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.MyShow
import com.michaldrabik.data_local.database.model.Show

interface MyShowsLocalDataSource {

  suspend fun getAll(): List<Show>

  suspend fun getAll(ids: List<Long>): List<Show>

  suspend fun getAllRecent(limit: Int): List<Show>

  suspend fun getAllTraktIds(): List<Long>

  suspend fun getById(tmdbId: Long): Show?

  suspend fun updateWatchedAt(
    tmdbId: Long,
    watchedAt: Long,
  )

  suspend fun insert(shows: List<MyShow>)

  suspend fun deleteById(tmdbId: Long)

  suspend fun checkExists(tmdbId: Long): Boolean
}
