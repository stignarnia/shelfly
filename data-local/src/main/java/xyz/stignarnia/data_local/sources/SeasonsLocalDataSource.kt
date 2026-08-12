package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.Season

interface SeasonsLocalDataSource {

  suspend fun getAll(tmdbIds: List<Long>): List<Season>

  suspend fun getAllByShowsIds(tmdbIds: List<Long>): List<Season>

  suspend fun getAllByShowsIdsChunk(tmdbIds: List<Long>): List<Season>

  suspend fun getAllWatched(): List<Season>

  suspend fun getAllWatchedForShows(tmdbIds: List<Long>): List<Season>

  suspend fun getAllWatchedIdsForShows(tmdbIds: List<Long>): List<Long>

  suspend fun getAllByShowId(tmdbId: Long): List<Season>

  suspend fun getById(tmdbId: Long): Season?

  suspend fun update(items: List<Season>)

  suspend fun upsert(items: List<Season>)

  suspend fun delete(items: List<Season>)

  suspend fun deleteAllForShow(showTmdbId: Long)
}
