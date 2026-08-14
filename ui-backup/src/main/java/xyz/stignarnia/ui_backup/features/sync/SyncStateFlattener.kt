package xyz.stignarnia.ui_backup.features.sync

import xyz.stignarnia.ui_backup.features.sync.model.SyncEntity
import xyz.stignarnia.ui_backup.model.BackupScheme

/**
 * Turns a full state snapshot into a flat set of keyed entities.
 *
 * Both halves of sync need the same view: the diff that derives deletions has
 * to compare two snapshots entity by entity, and the merge has to decide, per
 * entity, whether the newest thing it knows is an addition or a deletion.
 * Doing that against the nested backup scheme directly would mean repeating the
 * traversal in both places and letting them drift.
 */
internal object SyncStateFlattener {

  /**
   * Every entity present in [scheme], keyed by kind and stable id.
   *
   * The value is the entity's own timestamp where it has a meaningful one -
   * when it was added or last changed - so the merge can order it against a
   * deletion. Entities with no timestamp of their own report 0 and are ordered
   * purely by tombstones.
   */
  fun flatten(scheme: BackupScheme): Map<Pair<SyncEntity, String>, Long> {
    val entities = mutableMapOf<Pair<SyncEntity, String>, Long>()

    fun put(
      entity: SyncEntity,
      key: String,
      timestamp: Long,
    ) {
      // A thing listed twice keeps its newest sighting.
      val existing = entities[entity to key]
      if (existing == null || timestamp > existing) {
        entities[entity to key] = timestamp
      }
    }

    with(scheme.shows) {
      collectionHistory.forEach { put(SyncEntity.MY_SHOW, it.tmdbId.toString(), it.addedAt.toEpochMillis()) }
      collectionWatchlist.forEach {
        put(SyncEntity.WATCHLIST_SHOW, it.tmdbId.toString(), it.addedAt.toEpochMillis())
      }
      collectionHidden.forEach { put(SyncEntity.HIDDEN_SHOW, it.tmdbId.toString(), it.addedAt.toEpochMillis()) }

      progressEpisodes.forEach {
        val key = SyncEntity.EPISODE_WATCHED.key(it.showTmdbId, it.seasonNumber, it.episodeNumber)
        put(SyncEntity.EPISODE_WATCHED, key, it.addedAt?.toEpochMillis() ?: 0)
      }
      progressSeasons.forEach {
        put(SyncEntity.SEASON_WATCHED, SyncEntity.SEASON_WATCHED.key(it.showTmdbId, it.seasonNumber), 0)
      }

      progressPinned.forEach { put(SyncEntity.PINNED_SHOW, it.toString(), 0) }
      progressOnHold.forEach { put(SyncEntity.ON_HOLD_SHOW, it.toString(), 0) }

      ratingsShows.forEach { put(SyncEntity.SHOW_RATING, it.tmdbId.toString(), it.ratedAt.toEpochMillis()) }
      ratingsSeasons.forEach {
        val key = SyncEntity.SEASON_RATING.key(it.showTmdbId, it.seasonNumber)
        put(SyncEntity.SEASON_RATING, key, it.ratedAt.toEpochMillis())
      }
      ratingsEpisodes.forEach {
        val key = SyncEntity.EPISODE_RATING.key(it.showTmdbId, it.seasonNumber, it.episodeNumber)
        put(SyncEntity.EPISODE_RATING, key, it.ratedAt.toEpochMillis())
      }
    }

    with(scheme.movies) {
      collectionHistory.forEach { put(SyncEntity.MY_MOVIE, it.tmdbId.toString(), it.addedAt.toEpochMillis()) }
      collectionWatchlist.forEach {
        put(SyncEntity.WATCHLIST_MOVIE, it.tmdbId.toString(), it.addedAt.toEpochMillis())
      }
      collectionHidden.forEach { put(SyncEntity.HIDDEN_MOVIE, it.tmdbId.toString(), it.addedAt.toEpochMillis()) }
      progressPinned.forEach { put(SyncEntity.PINNED_MOVIE, it.toString(), 0) }
      ratingsMovies.forEach { put(SyncEntity.MOVIE_RATING, it.tmdbId.toString(), it.ratedAt.toEpochMillis()) }
    }

    scheme.lists.lists.forEach { list ->
      put(SyncEntity.CUSTOM_LIST, list.id.toString(), list.updatedAt.toEpochMillis())
      list.items.forEach { item ->
        val key = SyncEntity.CUSTOM_LIST_ITEM.key(list.id, item.type, item.tmdbId)
        put(SyncEntity.CUSTOM_LIST_ITEM, key, item.updatedAt.toEpochMillis())
      }
    }

    return entities
  }
}
