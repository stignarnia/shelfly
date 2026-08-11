package com.michaldrabik.data_remote.tmdb

import com.michaldrabik.data_remote.tmdb.model.TmdbEpisode
import com.michaldrabik.data_remote.tmdb.model.TmdbMovie
import com.michaldrabik.data_remote.tmdb.model.TmdbSearchItem
import com.michaldrabik.data_remote.tmdb.model.TmdbSeason
import com.michaldrabik.data_remote.tmdb.model.TmdbShow
import com.michaldrabik.data_remote.tmdb.model.TmdbVideos
import com.michaldrabik.data_remote.trakt.model.AirTime
import com.michaldrabik.data_remote.trakt.model.Episode
import com.michaldrabik.data_remote.trakt.model.Ids
import com.michaldrabik.data_remote.trakt.model.Movie
import com.michaldrabik.data_remote.trakt.model.Season
import com.michaldrabik.data_remote.trakt.model.Show

/**
 * Maps TMDB responses onto the data-remote DTOs the repository layer already
 * consumes, so switching data source does not ripple through the mappers in
 * :repository.
 *
 * These DTOs still live in the trakt.model package. They are plain data holders
 * with nothing Trakt-specific in them, and the package is renamed once the Trakt
 * source is deleted.
 */

private const val CERTIFICATION_COUNTRY = "US"
private const val VIDEO_SITE_YOUTUBE = "YouTube"
private const val VIDEO_TYPE_TRAILER = "Trailer"

internal fun TmdbShow.toShow(): Show =
  Show(
    ids = toIds(),
    title = name,
    year = first_air_date.toYear(),
    overview = overview,
    first_aired = first_air_date.toIsoInstant(),
    runtime = episode_run_time?.firstOrNull(),
    // TMDB exposes no airtime of day or timezone, only the air date. Episode
    // notifications therefore fire on the date rather than at the exact time.
    airs = AirTime(day = null, time = null, timezone = null),
    certification = content_ratings
      ?.results
      ?.firstOrNull { it.iso_3166_1 == CERTIFICATION_COUNTRY }
      ?.rating,
    network = networks?.firstOrNull()?.name,
    country = origin_country?.firstOrNull()?.lowercase(),
    trailer = videos.toTrailerUrl(),
    homepage = homepage,
    status = status?.lowercase(),
    rating = vote_average,
    votes = vote_count,
    comment_count = null,
    genres = genres?.mapNotNull { it.name?.lowercase() },
    aired_episodes = number_of_episodes,
  )

internal fun TmdbShow.toIds(): Ids =
  Ids(
    trakt = null,
    slug = null,
    tvdb = external_ids?.tvdb_id,
    imdb = external_ids?.imdb_id,
    tmdb = id,
    tvrage = null,
  )

internal fun TmdbMovie.toMovie(): Movie =
  Movie(
    ids = Ids(
      trakt = null,
      slug = null,
      tvdb = null,
      imdb = external_ids?.imdb_id,
      tmdb = id,
      tvrage = null,
    ),
    title = title,
    year = release_date.toYear(),
    overview = overview,
    released = release_date.toIsoInstant(),
    runtime = runtime,
    country = production_countries?.firstOrNull()?.iso_3166_1?.lowercase(),
    trailer = videos.toTrailerUrl(),
    homepage = homepage,
    status = status?.lowercase(),
    rating = vote_average,
    votes = vote_count,
    comment_count = null,
    genres = genres?.mapNotNull { it.name?.lowercase() },
    language = original_language,
  )

internal fun TmdbSeason.toSeason(showTmdbId: Long?): Season =
  Season(
    ids = Ids(
      trakt = null,
      slug = null,
      tvdb = null,
      imdb = null,
      tmdb = id,
      tvrage = null,
    ),
    number = season_number,
    episode_count = episode_count ?: episodes?.size,
    aired_episodes = episodes?.count { it.air_date.isAired() },
    title = name,
    first_aired = air_date.toIsoInstant(),
    overview = overview,
    rating = vote_average,
    episodes = episodes?.map { it.toEpisode(showTmdbId) },
  )

internal fun TmdbEpisode.toEpisode(showTmdbId: Long?): Episode =
  Episode(
    season = season_number,
    number = episode_number,
    title = name,
    ids = Ids(
      trakt = null,
      slug = null,
      tvdb = null,
      imdb = null,
      // TMDB episode ids are not stable across the API the way show ids are, so
      // episodes are addressed by (show, season, number) instead. The show id is
      // carried here so callers can resolve the parent.
      tmdb = showTmdbId,
      tvrage = null,
    ),
    overview = overview,
    rating = vote_average,
    votes = vote_count,
    comment_count = null,
    first_aired = air_date.toIsoInstant(),
    runtime = runtime,
    number_abs = null,
    last_watched_at = null,
  )

internal fun TmdbSearchItem.toShow(): Show =
  Show(
    ids = Ids(trakt = null, slug = null, tvdb = null, imdb = null, tmdb = id, tvrage = null),
    title = name,
    year = first_air_date.toYear(),
    overview = overview,
    first_aired = first_air_date.toIsoInstant(),
    runtime = null,
    airs = AirTime(day = null, time = null, timezone = null),
    certification = null,
    network = null,
    country = origin_country?.firstOrNull()?.lowercase(),
    trailer = null,
    homepage = null,
    status = null,
    rating = vote_average,
    votes = vote_count,
    comment_count = null,
    genres = null,
    aired_episodes = null,
  )

internal fun TmdbSearchItem.toMovie(): Movie =
  Movie(
    ids = Ids(trakt = null, slug = null, tvdb = null, imdb = null, tmdb = id, tvrage = null),
    title = title,
    year = release_date.toYear(),
    overview = overview,
    released = release_date.toIsoInstant(),
    runtime = null,
    country = null,
    trailer = null,
    homepage = null,
    status = null,
    rating = vote_average,
    votes = vote_count,
    comment_count = null,
    genres = null,
    language = null,
  )

private fun TmdbVideos?.toTrailerUrl(): String? =
  this
    ?.results
    ?.firstOrNull { it.site == VIDEO_SITE_YOUTUBE && it.type == VIDEO_TYPE_TRAILER }
    ?.key
    ?.let { "https://youtube.com/watch?v=$it" }

private fun String?.toYear(): Int? = this?.take(4)?.toIntOrNull()

private fun String?.isAired(): Boolean = !this.isNullOrBlank()

/**
 * TMDB returns plain dates ("2011-04-17") where Trakt returned full instants.
 * ZonedDateTime.parse rejects the former, so dates are widened to midnight UTC.
 */
private fun String?.toIsoInstant(): String? {
  if (this.isNullOrBlank()) {
    return null
  }
  return if (length == "yyyy-MM-dd".length) "${this}T00:00:00.000Z" else this
}
