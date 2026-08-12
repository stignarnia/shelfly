package xyz.stignarnia.ui_backup.features.import_.migrations.model

import com.squareup.moshi.Json

/**
 * Backup scheme v2, as written by Showly before this fork.
 *
 * Every entry is keyed by an id from the old catalog source ("id" on entries,
 * "sId" on children) which this fork cannot resolve. Some entries also carry a
 * TMDB id ("tmId"/"stmId"), written as -1 when the exporter could not find one.
 *
 * These models exist only to be read by [xyz.stignarnia.ui_backup.features.import_.migrations.BackupMigrationV2];
 * nothing else should depend on them.
 */
internal data class BackupSchemeV2(
  val version: Int,
  val platform: String,
  val createdAt: String,
  val shows: BackupShowsV2 = BackupShowsV2(),
  val movies: BackupMoviesV2 = BackupMoviesV2(),
  val lists: BackupListsV2 = BackupListsV2(),
)

// Shows

internal data class BackupShowsV2(
  @Json(name = "cH") val collectionHistory: List<BackupShowV2> = emptyList(),
  @Json(name = "cW") val collectionWatchlist: List<BackupShowV2> = emptyList(),
  @Json(name = "cHid") val collectionHidden: List<BackupShowV2> = emptyList(),
  @Json(name = "pEp") val progressEpisodes: List<BackupEpisodeV2> = emptyList(),
  @Json(name = "pSe") val progressSeasons: List<BackupSeasonV2> = emptyList(),
  @Json(name = "pP") val progressPinned: List<Long> = emptyList(),
  @Json(name = "pOH") val progressOnHold: List<Long> = emptyList(),
  @Json(name = "rS") val ratingsShows: List<BackupShowRatingV2> = emptyList(),
  @Json(name = "rSe") val ratingsSeasons: List<BackupSeasonRatingV2> = emptyList(),
  @Json(name = "rEp") val ratingsEpisodes: List<BackupEpisodeRatingV2> = emptyList(),
)

internal data class BackupShowV2(
  @Json(name = "id") val legacyId: Long,
  @Json(name = "tmId") val tmdbId: Long = -1,
  @Json(name = "t") val title: String,
  @Json(name = "a") val addedAt: String,
  @Json(name = "u") val updatedAt: String,
)

internal data class BackupSeasonV2(
  @Json(name = "sId") val showLegacyId: Long = -1,
  @Json(name = "stmId") val showTmdbId: Long = -1,
  @Json(name = "sN") val seasonNumber: Int,
)

/**
 * "stmId" is deliberately not read here. The v2 exporter copied it from the
 * episode row's own show-TMDB column, which was never populated correctly, so
 * the value points at something other than the parent show. The parent is
 * recovered from "sId" instead. Seasons and show ratings are unaffected: their
 * exporter looked the id up properly.
 */
internal data class BackupEpisodeV2(
  @Json(name = "sId") val showLegacyId: Long = -1,
  @Json(name = "eN") val episodeNumber: Int,
  @Json(name = "sN") val seasonNumber: Int,
  @Json(name = "a") val addedAt: String?,
)

internal data class BackupShowRatingV2(
  @Json(name = "id") val showLegacyId: Long = -1,
  @Json(name = "tmId") val showTmdbId: Long = -1,
  @Json(name = "r") val rating: Int,
  @Json(name = "rA") val ratedAt: String,
)

internal data class BackupSeasonRatingV2(
  @Json(name = "sId") val showLegacyId: Long = -1,
  @Json(name = "stmId") val showTmdbId: Long = -1,
  @Json(name = "sN") val seasonNumber: Int,
  @Json(name = "r") val rating: Int,
  @Json(name = "rA") val ratedAt: String,
)

internal data class BackupEpisodeRatingV2(
  @Json(name = "sId") val showLegacyId: Long = -1,
  @Json(name = "sN") val seasonNumber: Int,
  @Json(name = "eN") val episodeNumber: Int,
  @Json(name = "r") val rating: Int,
  @Json(name = "rA") val ratedAt: String,
)

// Movies

internal data class BackupMoviesV2(
  @Json(name = "cH") val collectionHistory: List<BackupMovieV2> = emptyList(),
  @Json(name = "cW") val collectionWatchlist: List<BackupMovieV2> = emptyList(),
  @Json(name = "cHid") val collectionHidden: List<BackupMovieV2> = emptyList(),
  @Json(name = "pP") val progressPinned: List<Long> = emptyList(),
  @Json(name = "rM") val ratingsMovies: List<BackupMovieRatingV2> = emptyList(),
)

internal data class BackupMovieV2(
  @Json(name = "id") val legacyId: Long,
  @Json(name = "tmId") val tmdbId: Long = -1,
  @Json(name = "t") val title: String,
  @Json(name = "a") val addedAt: String,
)

internal data class BackupMovieRatingV2(
  @Json(name = "id") val movieLegacyId: Long = -1,
  @Json(name = "tmId") val movieTmdbId: Long = -1,
  @Json(name = "r") val rating: Int,
  @Json(name = "rA") val ratedAt: String,
)

// Lists

internal data class BackupListsV2(
  @Json(name = "l") val lists: List<BackupListV2> = emptyList(),
)

internal data class BackupListV2(
  @Json(name = "id") val id: Long,
  @Json(name = "sId") val slugId: String,
  @Json(name = "n") val name: String,
  @Json(name = "d") val description: String?,
  @Json(name = "p") val privacy: String,
  @Json(name = "ic") val itemCount: Long,
  @Json(name = "c") val createdAt: String,
  @Json(name = "u") val updatedAt: String,
  @Json(name = "it") val items: List<BackupListItemV2> = emptyList(),
)

internal data class BackupListItemV2(
  @Json(name = "id") val id: Long,
  @Json(name = "lId") val listId: Long,
  @Json(name = "tId") val legacyId: Long = -1,
  @Json(name = "tmId") val tmdbId: Long = -1,
  @Json(name = "t") val type: String,
  @Json(name = "r") val rank: Long,
  @Json(name = "l") val listedAt: String,
  @Json(name = "c") val createdAt: String,
  @Json(name = "u") val updatedAt: String,
)
