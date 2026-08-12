@file:Suppress("ktlint:standard:max-line-length")

package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.Episode

interface EpisodesLocalDataSource {

  suspend fun upsert(episodes: List<Episode>)

  suspend fun upsertChunked(items: List<Episode>)

  suspend fun updateIsExported(
    episodesIds: List<Long>,
    exportedAt: Long,
  )

  suspend fun isEpisodeWatched(
    showTmdbId: Long,
    episodeTmdbId: Long,
  ): Boolean

  suspend fun getById(
    showTmdbId: Long,
    episodeTmdbId: Long,
  ): Episode?

  suspend fun getAll(episodesIds: List<Long>): List<Episode>

  suspend fun getAllForSeason(seasonTmdbId: Long): List<Episode>

  suspend fun getAllByShowId(showTmdbId: Long): List<Episode>

  suspend fun getAllByShowId(
    showTmdbId: Long,
    seasonNumber: Int,
  ): List<Episode>

  suspend fun getAllByShowsIds(showTmdbIds: List<Long>): List<Episode>

  suspend fun getAllByShowsIdsChunk(showTmdbIds: List<Long>): List<Episode>

  suspend fun getFirstUnwatched(
    showTmdbId: Long,
    toTime: Long,
  ): Episode?

  suspend fun getFirstUnwatched(
    showTmdbId: Long,
    fromTime: Long,
    toTime: Long,
  ): Episode?

  suspend fun getFirstUnwatchedAfterEpisode(
    showTmdbId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
    toTime: Long,
  ): Episode?

  suspend fun getLastWatched(showTmdbId: Long): Episode?

  suspend fun getTotalCount(
    showTmdbId: Long,
    toTime: Long,
  ): Int

  suspend fun getTotalCount(showTmdbId: Long): Int

  suspend fun getWatchedCount(
    showTmdbId: Long,
    toTime: Long,
  ): Int

  suspend fun getWatchedCount(showTmdbId: Long): Int

  suspend fun getAllWatched(): List<Episode>

  suspend fun getAllWatchedForShows(showsIds: List<Long>): List<Episode>

  suspend fun getAllWatchedForShows(
    showsIds: List<Long>,
    fromTime: Long,
    toTime: Long,
  ): List<Episode>

  suspend fun getAllWatchedIdsForShows(showsIds: List<Long>): List<Long>

  suspend fun deleteAllUnwatchedForShow(showTmdbId: Long)

  suspend fun deleteAllForShow(showTmdbId: Long)

  suspend fun delete(items: List<Episode>)
}
