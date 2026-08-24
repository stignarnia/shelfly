package xyz.stignarnia.ui_backup.features.import_.migrations.model

import com.squareup.moshi.Json

/**
 * Backup scheme v2, as written by Showly before this fork.
 *
 * Every entry is keyed by an id from the old catalog source ("id" on entries, "sId" on children) which this fork cannot resolve.
 * Some entries also carry a TMDB id ("tmId"/"stmId"), written as -1 when the exporter could not find one.
 *
 * These models exist only to be read by [xyz.stignarnia.ui_backup.features.import_.migrations.BackupMigrationV2]; nothing else should depend on them.
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
  @param:Json(name = "cH") val collectionHistory: List<BackupShowV2> = emptyList(),
  @param:Json(name = "cW") val collectionWatchlist: List<BackupShowV2> = emptyList(),
  @param:Json(name = "cHid") val collectionHidden: List<BackupShowV2> = emptyList(),
  @param:Json(name = "pEp") val progressEpisodes: List<BackupEpisodeV2> = emptyList(),
  @param:Json(name = "pSe") val progressSeasons: List<BackupSeasonV2> = emptyList(),
  @param:Json(name = "pP") val progressPinned: List<Long> = emptyList(),
  @param:Json(name = "pOH") val progressOnHold: List<Long> = emptyList(),
  @param:Json(name = "rS") val ratingsShows: List<BackupShowRatingV2> = emptyList(),
  @param:Json(name = "rSe") val ratingsSeasons: List<BackupSeasonRatingV2> = emptyList(),
  @param:Json(name = "rEp") val ratingsEpisodes: List<BackupEpisodeRatingV2> = emptyList(),
)

internal data class BackupShowV2(
  @param:Json(name = "id") val legacyId: Long,
  @param:Json(name = "tmId") val tmdbId: Long = -1,
  @param:Json(name = "t") val title: String,
  @param:Json(name = "a") val addedAt: String,
  @param:Json(name = "u") val updatedAt: String,
)

internal data class BackupSeasonV2(
  @param:Json(name = "sId") val showLegacyId: Long = -1,
  @param:Json(name = "stmId") val showTmdbId: Long = -1,
  @param:Json(name = "sN") val seasonNumber: Int,
)

/**
 * "stmId" is deliberately not read here.
 * The v2 exporter copied it from the episode row's own show-TMDB column, which was never populated correctly, so the value points at something other than the parent show.
 * The parent is recovered from "sId" instead.
 * Seasons and show ratings are unaffected: their exporter looked the id up properly.
 */
internal data class BackupEpisodeV2(
  @param:Json(name = "sId") val showLegacyId: Long = -1,
  @param:Json(name = "eN") val episodeNumber: Int,
  @param:Json(name = "sN") val seasonNumber: Int,
  @param:Json(name = "a") val addedAt: String?,
)

internal data class BackupShowRatingV2(
  @param:Json(name = "id") val showLegacyId: Long = -1,
  @param:Json(name = "tmId") val showTmdbId: Long = -1,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)

internal data class BackupSeasonRatingV2(
  @param:Json(name = "sId") val showLegacyId: Long = -1,
  @param:Json(name = "stmId") val showTmdbId: Long = -1,
  @param:Json(name = "sN") val seasonNumber: Int,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)

internal data class BackupEpisodeRatingV2(
  @param:Json(name = "sId") val showLegacyId: Long = -1,
  @param:Json(name = "sN") val seasonNumber: Int,
  @param:Json(name = "eN") val episodeNumber: Int,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)

// Movies

internal data class BackupMoviesV2(
  @param:Json(name = "cH") val collectionHistory: List<BackupMovieV2> = emptyList(),
  @param:Json(name = "cW") val collectionWatchlist: List<BackupMovieV2> = emptyList(),
  @param:Json(name = "cHid") val collectionHidden: List<BackupMovieV2> = emptyList(),
  @param:Json(name = "pP") val progressPinned: List<Long> = emptyList(),
  @param:Json(name = "rM") val ratingsMovies: List<BackupMovieRatingV2> = emptyList(),
)

internal data class BackupMovieV2(
  @param:Json(name = "id") val legacyId: Long,
  @param:Json(name = "tmId") val tmdbId: Long = -1,
  @param:Json(name = "t") val title: String,
  @param:Json(name = "a") val addedAt: String,
)

internal data class BackupMovieRatingV2(
  @param:Json(name = "id") val movieLegacyId: Long = -1,
  @param:Json(name = "tmId") val movieTmdbId: Long = -1,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)

// Lists

internal data class BackupListsV2(
  @param:Json(name = "l") val lists: List<BackupListV2> = emptyList(),
)

internal data class BackupListV2(
  @param:Json(name = "id") val id: Long,
  @param:Json(name = "sId") val slugId: String,
  @param:Json(name = "n") val name: String,
  @param:Json(name = "d") val description: String?,
  @param:Json(name = "p") val privacy: String,
  @param:Json(name = "ic") val itemCount: Long,
  @param:Json(name = "c") val createdAt: String,
  @param:Json(name = "u") val updatedAt: String,
  @param:Json(name = "it") val items: List<BackupListItemV2> = emptyList(),
)

internal data class BackupListItemV2(
  @param:Json(name = "id") val id: Long,
  @param:Json(name = "lId") val listId: Long,
  @param:Json(name = "tId") val legacyId: Long = -1,
  @param:Json(name = "tmId") val tmdbId: Long = -1,
  @param:Json(name = "t") val type: String,
  @param:Json(name = "r") val rank: Long,
  @param:Json(name = "l") val listedAt: String,
  @param:Json(name = "c") val createdAt: String,
  @param:Json(name = "u") val updatedAt: String,
)
