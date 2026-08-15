package xyz.stignarnia.data_remote.tmdb

import xyz.stignarnia.data_remote.tmdb.model.TmdbEpisode
import xyz.stignarnia.data_remote.tmdb.model.TmdbMovie
import xyz.stignarnia.data_remote.tmdb.model.TmdbSearchItem
import xyz.stignarnia.data_remote.tmdb.model.TmdbSeason
import xyz.stignarnia.data_remote.tmdb.model.TmdbShow
import xyz.stignarnia.data_remote.tmdb.model.TmdbVideos
import xyz.stignarnia.data_remote.catalog.model.AirTime
import xyz.stignarnia.data_remote.catalog.model.Episode
import xyz.stignarnia.data_remote.catalog.model.Ids
import xyz.stignarnia.data_remote.catalog.model.Movie
import xyz.stignarnia.data_remote.catalog.model.Season
import xyz.stignarnia.data_remote.catalog.model.Show

/**
 * Maps TMDB responses onto the shared catalog DTOs the repository layer
 * consumes.
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
    genres = genres?.mapNotNull { it.name?.lowercase() }
      ?: TmdbGenres.showSlugs(genre_ids.orEmpty()),
    aired_episodes = number_of_episodes,
  )

internal fun TmdbShow.toIds(): Ids =
  Ids(
    slug = null,
    tvdb = external_ids?.tvdb_id,
    imdb = external_ids?.imdb_id,
    tmdb = id,
    tvrage = null,
  )

internal fun TmdbMovie.toMovie(): Movie =
  Movie(
    ids = Ids(
      slug = null,
      tvdb = null,
      imdb = external_ids?.imdb_id,
      tmdb = id,
      tvrage = null,
    ),
    title = title,
    year = release_date.toYear(),
    overview = overview,
    released = release_date.toReleaseDate(),
    runtime = runtime,
    country = production_countries?.firstOrNull()?.iso_3166_1?.lowercase(),
    trailer = videos.toTrailerUrl(),
    homepage = homepage,
    status = status?.lowercase(),
    rating = vote_average,
    votes = vote_count,
    comment_count = null,
    genres = genres?.mapNotNull { it.name?.lowercase() }
      ?: TmdbGenres.movieSlugs(genre_ids.orEmpty()),
    language = original_language,
  )

internal fun TmdbSeason.toSeason(): Season =
  Season(
    ids = Ids(
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
    episodes = episodes?.map { it.toEpisode() },
  )

internal fun TmdbEpisode.toEpisode(): Episode =
  Episode(
    season = season_number,
    number = episode_number,
    title = name,
    ids = Ids(
      slug = null,
      tvdb = null,
      imdb = null,
      tmdb = id,
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
    ids = Ids(slug = null, tvdb = null, imdb = null, tmdb = id, tvrage = null),
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
    genres = TmdbGenres.showSlugs(genre_ids.orEmpty()),
    aired_episodes = null,
  )

internal fun TmdbSearchItem.toMovie(): Movie =
  Movie(
    ids = Ids(slug = null, tvdb = null, imdb = null, tmdb = id, tvrage = null),
    title = title,
    year = release_date.toYear(),
    overview = overview,
    released = release_date.toReleaseDate(),
    runtime = null,
    country = null,
    trailer = null,
    homepage = null,
    status = null,
    rating = vote_average,
    votes = vote_count,
    comment_count = null,
    genres = TmdbGenres.movieSlugs(genre_ids.orEmpty()),
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
 * TMDB returns plain dates ("2011-04-17") rather than full instants, and
 * ZonedDateTime.parse rejects those, so dates are widened to midnight UTC.
 */
private fun String?.toIsoInstant(): String? {
  if (this.isNullOrBlank()) {
    return null
  }
  return if (length == "yyyy-MM-dd".length) "${this}T00:00:00.000Z" else this
}

/**
 * A movie's release date stays a plain yyyy-MM-dd. Unlike a show's first_aired,
 * which is carried around as an ISO instant, every consumer of [Movie.released]
 * parses it with LocalDate, so widening it to an instant makes the parse throw.
 */
private fun String?.toReleaseDate(): String? = if (this.isNullOrBlank()) null else this
