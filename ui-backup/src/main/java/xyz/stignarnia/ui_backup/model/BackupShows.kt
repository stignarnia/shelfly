package xyz.stignarnia.ui_backup.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Backup scheme v3.
 *
 * The legacy id fields that v2 wrote ("id" on entries, "sId" on children) are gone: they came from the previous catalog source.
 * Entries are identified by their TMDB id, and children by their parent show plus season/episode numbers.
 *
 * The JSON keys are otherwise unchanged from v2, so a v2 file still reads - see the importer, which remaps the legacy ids on the way in.
 */
@JsonClass(generateAdapter = true)
data class BackupShows(
  @param:Json(name = "cH") val collectionHistory: List<BackupShow> = emptyList(),
  @param:Json(name = "cW") val collectionWatchlist: List<BackupShow> = emptyList(),
  @param:Json(name = "cHid") val collectionHidden: List<BackupShow> = emptyList(),
  @param:Json(name = "pEp") val progressEpisodes: List<BackupEpisode> = emptyList(),
  @param:Json(name = "pSe") val progressSeasons: List<BackupSeason> = emptyList(),
  @param:Json(name = "pP") val progressPinned: List<Long> = emptyList(),
  @param:Json(name = "pOH") val progressOnHold: List<Long> = emptyList(),
  @param:Json(name = "rS") val ratingsShows: List<BackupShowRating> = emptyList(),
  @param:Json(name = "rSe") val ratingsSeasons: List<BackupSeasonRating> = emptyList(),
  @param:Json(name = "rEp") val ratingsEpisodes: List<BackupEpisodeRating> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupShow(
  @param:Json(name = "tmId") val tmdbId: Long,
  @param:Json(name = "t") val title: String,
  @param:Json(name = "a") val addedAt: String,
  @param:Json(name = "u") val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class BackupSeason(
  @param:Json(name = "stmId") val showTmdbId: Long,
  @param:Json(name = "sN") val seasonNumber: Int,
)

@JsonClass(generateAdapter = true)
data class BackupEpisode(
  @param:Json(name = "stmId") val showTmdbId: Long,
  @param:Json(name = "eN") val episodeNumber: Int,
  @param:Json(name = "sN") val seasonNumber: Int,
  @param:Json(name = "a") val addedAt: String?,
)

// Ratings

@JsonClass(generateAdapter = true)
data class BackupShowRating(
  @param:Json(name = "tmId") val tmdbId: Long,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)

@JsonClass(generateAdapter = true)
data class BackupSeasonRating(
  @param:Json(name = "stmId") val showTmdbId: Long,
  @param:Json(name = "sN") val seasonNumber: Int,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)

@JsonClass(generateAdapter = true)
data class BackupEpisodeRating(
  @param:Json(name = "stmId") val showTmdbId: Long,
  @param:Json(name = "sN") val seasonNumber: Int,
  @param:Json(name = "eN") val episodeNumber: Int,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)
