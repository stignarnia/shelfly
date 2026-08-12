package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.Show
import xyz.stignarnia.data_local.database.model.ShowSearch

interface ShowsLocalDataSource {

  suspend fun getAll(): List<Show>

  suspend fun getAllForSearch(): List<ShowSearch>

  suspend fun getAll(ids: List<Long>): List<Show>

  suspend fun getAllTmdbIds(tmdbIds: List<Long>): Map<Long, Long>

  suspend fun getAllChunked(ids: List<Long>): List<Show>

  suspend fun getById(tmdbId: Long): Show?

  suspend fun getByTmdbId(tmdbId: Long): Show?

  suspend fun getBySlug(slug: String): Show?

  suspend fun getById(imdbId: String): Show?

  suspend fun deleteById(tmdbId: Long)

  suspend fun upsert(shows: List<Show>)
}
