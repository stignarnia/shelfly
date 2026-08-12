package xyz.stignarnia.common

import java.util.concurrent.TimeUnit.MINUTES

object ConfigVariant {

  val SHOW_SYNC_COOLDOWN by lazy { MINUTES.toMillis(60) }
  val MOVIE_SYNC_COOLDOWN by lazy { MINUTES.toMillis(60) }
  val TRANSLATION_SYNC_SHOW_MOVIE_COOLDOWN by lazy { MINUTES.toMillis(15) }
  val TRANSLATION_SYNC_EPISODE_COOLDOWN by lazy { MINUTES.toMillis(15) }

  val RATINGS_CACHE_DURATION by lazy { MINUTES.toMillis(3) }
  val STREAMINGS_CACHE_DURATION by lazy { MINUTES.toMillis(3) }
  val COLLECTIONS_CACHE_DURATION by lazy { MINUTES.toMillis(3) }
}
