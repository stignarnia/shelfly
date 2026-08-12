package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.ArchiveShow
import com.michaldrabik.data_local.database.model.Show

interface ArchiveShowsLocalDataSource {

  suspend fun getAll(): List<Show>

  suspend fun getAll(ids: List<Long>): List<Show>

  suspend fun getAllTmdbIds(): List<Long>

  suspend fun getById(tmdbId: Long): Show?

  suspend fun insert(show: ArchiveShow)

  suspend fun deleteById(tmdbId: Long)
}
