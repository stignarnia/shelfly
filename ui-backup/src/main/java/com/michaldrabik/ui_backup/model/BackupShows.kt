package com.michaldrabik.ui_backup.model

import com.squareup.moshi.Json

/**
 * Backup scheme v3.
 *
 * The legacy id fields that v2 wrote ("id" on entries, "sId" on children) are
 * gone: they came from the previous catalog source. Entries are identified by
 * their TMDB id, and children by their parent show plus season/episode numbers.
 *
 * The JSON keys are otherwise unchanged from v2, so a v2 file still reads - see
 * the importer, which remaps the legacy ids on the way in.
 */
data class BackupShows(
  @Json(name = "cH") val collectionHistory: List<BackupShow> = emptyList(),
  @Json(name = "cW") val collectionWatchlist: List<BackupShow> = emptyList(),
  @Json(name = "cHid") val collectionHidden: List<BackupShow> = emptyList(),
  @Json(name = "pEp") val progressEpisodes: List<BackupEpisode> = emptyList(),
  @Json(name = "pSe") val progressSeasons: List<BackupSeason> = emptyList(),
  @Json(name = "pP") val progressPinned: List<Long> = emptyList(),
  @Json(name = "pOH") val progressOnHold: List<Long> = emptyList(),
  @Json(name = "rS") val ratingsShows: List<BackupShowRating> = emptyList(),
  @Json(name = "rSe") val ratingsSeasons: List<BackupSeasonRating> = emptyList(),
  @Json(name = "rEp") val ratingsEpisodes: List<BackupEpisodeRating> = emptyList(),
)

data class BackupShow(
  @Json(name = "tmId") val tmdbId: Long,
  @Json(name = "t") val title: String,
  @Json(name = "a") val addedAt: String,
  @Json(name = "u") val updatedAt: String,
)

data class BackupSeason(
  @Json(name = "stmId") val showTmdbId: Long,
  @Json(name = "sN") val seasonNumber: Int,
)

data class BackupEpisode(
  @Json(name = "stmId") val showTmdbId: Long,
  @Json(name = "eN") val episodeNumber: Int,
  @Json(name = "sN") val seasonNumber: Int,
  @Json(name = "a") val addedAt: String?,
)

// Ratings

data class BackupShowRating(
  @Json(name = "tmId") val tmdbId: Long,
  @Json(name = "r") val rating: Int,
  @Json(name = "rA") val ratedAt: String,
)

data class BackupSeasonRating(
  @Json(name = "stmId") val showTmdbId: Long,
  @Json(name = "sN") val seasonNumber: Int,
  @Json(name = "r") val rating: Int,
  @Json(name = "rA") val ratedAt: String,
)

data class BackupEpisodeRating(
  @Json(name = "stmId") val showTmdbId: Long,
  @Json(name = "sN") val seasonNumber: Int,
  @Json(name = "eN") val episodeNumber: Int,
  @Json(name = "r") val rating: Int,
  @Json(name = "rA") val ratedAt: String,
)
