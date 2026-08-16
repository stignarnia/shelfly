package xyz.stignarnia.common

import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS

object Config {
  private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
  const val TMDB_IMAGE_BASE_POSTER_URL = "${TMDB_IMAGE_BASE_URL}w342"
  const val TMDB_IMAGE_BASE_FANART_URL = "${TMDB_IMAGE_BASE_URL}w1280"
  const val TMDB_IMAGE_BASE_PROFILE_URL = "${TMDB_IMAGE_BASE_URL}w1280"
  const val TMDB_IMAGE_BASE_PROFILE_THUMB_URL = "${TMDB_IMAGE_BASE_URL}w342"
  const val TMDB_IMAGE_BASE_STILL_URL = "${TMDB_IMAGE_BASE_URL}original"
  const val TMDB_IMAGE_BASE_ACTOR_URL = "${TMDB_IMAGE_BASE_URL}h632"
  const val TMDB_IMAGE_BASE_LOGO_URL = "${TMDB_IMAGE_BASE_URL}original"

  const val JUST_WATCH_URL = "https://www.justwatch.com/"
  const val TMDB_URL = "https://www.themoviedb.org/"

  const val MAIN_GRID_SPAN = 3
  const val MAIN_GRID_SPAN_TABLET = 6
  const val LISTS_GRID_SPAN = 4
  const val IMAGE_FADE_DURATION_MS = 150
  const val SEARCH_RECENTS_AMOUNT = 5
  const val FANART_GALLERY_IMAGES_LIMIT = 20
  const val DEFAULT_LANGUAGE = "en"
  const val DEFAULT_COUNTRY = "us"
  const val DEFAULT_DATE_FORMAT = "DEFAULT_24"
  const val DEFAULT_LIST_VIEW_MODE = "LIST_NORMAL"
  const val DEFAULT_LISTS_GRID_SPAN = 2
  const val HOST_ACTIVITY_NAME = "xyz.stignarnia.shelfly.ui.main.MainActivity"

  const val SHOW_WHATS_NEW = true

  val PROGRESS_UPCOMING_OPTIONS = listOf(0, 7, 14, 30, 45, 60, 90)
  val MY_SHOWS_RECENTS_OPTIONS = listOf(0, 2, 4, 6, 8)
  val DISCOVER_SHOWS_CACHE_DURATION by lazy { HOURS.toMillis(12) }
  val DISCOVER_MOVIES_CACHE_DURATION by lazy { HOURS.toMillis(12) }
  val RELATED_CACHE_DURATION by lazy { DAYS.toMillis(7) }
  val SHOW_DETAILS_CACHE_DURATION by lazy { DAYS.toMillis(3) }
  val MOVIE_DETAILS_CACHE_DURATION by lazy { DAYS.toMillis(3) }
  val ACTORS_CACHE_DURATION by lazy { DAYS.toMillis(5) }
  val NEW_BADGE_DURATION by lazy { DAYS.toMillis(3) }
  val PEOPLE_CREDITS_CACHE_DURATION by lazy { DAYS.toMillis(7) }
  val PEOPLE_IMAGES_CACHE_DURATION by lazy { DAYS.toMillis(7) }

  const val SPOILERS_HIDE_SYMBOL = "•"
  const val SPOILERS_RATINGS_HIDE_SYMBOL = "•.•"
  const val SPOILERS_RATINGS_VOTES_HIDE_SYMBOL = "•.• (•••••)"
  val SPOILERS_REGEX = "\\S".toRegex()
}
