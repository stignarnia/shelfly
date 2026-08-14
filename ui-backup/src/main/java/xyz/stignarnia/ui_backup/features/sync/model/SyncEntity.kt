package xyz.stignarnia.ui_backup.features.sync.model

/**
 * The kinds of user state that sync, and how each one is identified.
 *
 * Every entity needs a key that is stable across devices, so a TMDB id rather
 * than a local row id. Membership of a collection is its own entity: a show
 * moved from the watchlist into the collection is a deletion in one and an
 * addition in the other, and both halves have to travel.
 */
enum class SyncEntity {
  MY_SHOW,
  WATCHLIST_SHOW,
  HIDDEN_SHOW,
  MY_MOVIE,
  WATCHLIST_MOVIE,
  HIDDEN_MOVIE,
  EPISODE_WATCHED,
  SEASON_WATCHED,
  SHOW_RATING,
  SEASON_RATING,
  EPISODE_RATING,
  MOVIE_RATING,
  CUSTOM_LIST,
  CUSTOM_LIST_ITEM,
  PINNED_SHOW,
  PINNED_MOVIE,
  ON_HOLD_SHOW,
  ;

  fun key(vararg parts: Any): String = parts.joinToString(separator = ":")
}

/**
 * A deletion, as it travels between devices.
 *
 * [deletedAt] is a lower bound rather than the exact moment: deletions are
 * derived by comparing local state against what this device last published, so
 * all that is known is that it happened after that sync. Taking the earlier
 * bound means a genuine re-add on another device wins the tie, which is the
 * failure mode worth having - a resurrected show is a nuisance, a wrongly
 * deleted collection is not.
 */
data class SyncTombstoneEntry(
  val entity: SyncEntity,
  val key: String,
  val deletedAt: Long,
)
