package xyz.stignarnia.ui_backup.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupMovies(
  @param:Json(name = "cH") val collectionHistory: List<BackupMovie> = emptyList(),
  @param:Json(name = "cW") val collectionWatchlist: List<BackupMovie> = emptyList(),
  @param:Json(name = "cHid") val collectionHidden: List<BackupMovie> = emptyList(),
  @param:Json(name = "pP") val progressPinned: List<Long> = emptyList(),
  @param:Json(name = "rM") val ratingsMovies: List<BackupMovieRating> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupMovie(
  @param:Json(name = "tmId") val tmdbId: Long,
  @param:Json(name = "t") val title: String,
  @param:Json(name = "a") val addedAt: String,
)

// Ratings

@JsonClass(generateAdapter = true)
data class BackupMovieRating(
  @param:Json(name = "tmId") val tmdbId: Long,
  @param:Json(name = "r") val rating: Int,
  @param:Json(name = "rA") val ratedAt: String,
)
