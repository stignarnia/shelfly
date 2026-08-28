package xyz.stignarnia.uiBackup.features.sync

import xyz.stignarnia.uiBackup.features.sync.model.SyncEntity
import xyz.stignarnia.uiBackup.features.sync.model.SyncPayload
import xyz.stignarnia.uiBackup.features.sync.model.SyncTombstoneEntry
import xyz.stignarnia.uiBackup.model.BackupList
import xyz.stignarnia.uiBackup.model.BackupLists
import xyz.stignarnia.uiBackup.model.BackupMovies
import xyz.stignarnia.uiBackup.model.BackupScheme
import xyz.stignarnia.uiBackup.model.BackupShows

/**
 * Reconciles this device's state with every peer's.
 *
 * Pure by design - no database, no network, no clock.
 * Everything that decides whether your watch history survives a sync is decided here, and this is the only part of the feature that can be tested exhaustively without two real devices and a server.
 *
 * The rules, in order of how much trouble each one prevents:
 *
 * - **Union first.**
 * An entity present on any device is present in the result.
 * - **Absence is never deletion.**
 * Only a tombstone removes something.
 * A device that has been offline for a month has a stale, small state file; without this rule it would delete everything it had not heard about.
 * - **A tombstone wins only if it is newer** than the newest sighting of the entity.
 * That is what lets a deletion propagate, and equally what lets a deliberate re-add on another device overrule an older deletion.
 * - **Ties go to keeping the data.**
 * A tombstone must be strictly newer to win.
 */
internal object SyncMerge {
  data class Result(
    val state: BackupScheme,
    /** Every deletion still being vouched for, ours and our peers'. */
    val tombstones: List<SyncTombstoneEntry>,
  )

  fun merge(
    local: BackupScheme,
    localTombstones: List<SyncTombstoneEntry>,
    peers: List<SyncPayload>,
  ): Result {
    val states = listOf(local) + peers.map { it.state }
    val tombstones = newestPerKey(localTombstones + peers.flatMap { it.tombstones })

    // The newest time any device saw each entity, used to decide whether a deletion happened before or after the thing it claims to delete.
    val sightings = mutableMapOf<Pair<SyncEntity, String>, Long>()
    states.forEach { state ->
      SyncStateFlattener.flatten(state).forEach { (key, timestamp) ->
        val existing = sightings[key]
        if (existing == null || timestamp > existing) sightings[key] = timestamp
      }
    }

    fun survives(
      entity: SyncEntity,
      key: String,
    ): Boolean {
      val deletedAt = tombstones[entity to key] ?: return true
      val seenAt = sightings[entity to key] ?: 0
      // Strictly newer, so a tie keeps the data rather than deleting it.
      return seenAt >= deletedAt
    }

    val shows =
      BackupShows(
        collectionHistory =
          states
            .flatMap { it.shows.collectionHistory }
            .mergeBy(SyncEntity.MY_SHOW, { it.tmdbId.toString() }, { it.addedAt.toEpochMillis() }, ::survives),
        collectionWatchlist =
          states
            .flatMap { it.shows.collectionWatchlist }
            .mergeBy(SyncEntity.WATCHLIST_SHOW, { it.tmdbId.toString() }, { it.addedAt.toEpochMillis() }, ::survives),
        collectionHidden =
          states
            .flatMap { it.shows.collectionHidden }
            .mergeBy(SyncEntity.HIDDEN_SHOW, { it.tmdbId.toString() }, { it.addedAt.toEpochMillis() }, ::survives),
        progressEpisodes =
          states
            .flatMap { it.shows.progressEpisodes }
            .mergeBy(
              SyncEntity.EPISODE_WATCHED,
              { SyncEntity.EPISODE_WATCHED.key(it.showTmdbId, it.seasonNumber, it.episodeNumber) },
              { it.addedAt.toEpochMillis() },
              ::survives,
            ),
        progressSeasons =
          states
            .flatMap { it.shows.progressSeasons }
            .mergeBy(
              SyncEntity.SEASON_WATCHED,
              { SyncEntity.SEASON_WATCHED.key(it.showTmdbId, it.seasonNumber) },
              { 0 },
              ::survives,
            ),
        progressPinned =
          states
            .flatMap { it.shows.progressPinned }
            .mergeBy(SyncEntity.PINNED_SHOW, { it.toString() }, { 0 }, ::survives),
        progressOnHold =
          states
            .flatMap { it.shows.progressOnHold }
            .mergeBy(SyncEntity.ON_HOLD_SHOW, { it.toString() }, { 0 }, ::survives),
        ratingsShows =
          states
            .flatMap { it.shows.ratingsShows }
            .mergeBy(SyncEntity.SHOW_RATING, { it.tmdbId.toString() }, { it.ratedAt.toEpochMillis() }, ::survives),
        ratingsSeasons =
          states
            .flatMap { it.shows.ratingsSeasons }
            .mergeBy(
              SyncEntity.SEASON_RATING,
              { SyncEntity.SEASON_RATING.key(it.showTmdbId, it.seasonNumber) },
              { it.ratedAt.toEpochMillis() },
              ::survives,
            ),
        ratingsEpisodes =
          states
            .flatMap { it.shows.ratingsEpisodes }
            .mergeBy(
              SyncEntity.EPISODE_RATING,
              { SyncEntity.EPISODE_RATING.key(it.showTmdbId, it.seasonNumber, it.episodeNumber) },
              { it.ratedAt.toEpochMillis() },
              ::survives,
            ),
      )

    val movies =
      BackupMovies(
        collectionHistory =
          states
            .flatMap { it.movies.collectionHistory }
            .mergeBy(SyncEntity.MY_MOVIE, { it.tmdbId.toString() }, { it.addedAt.toEpochMillis() }, ::survives),
        collectionWatchlist =
          states
            .flatMap { it.movies.collectionWatchlist }
            .mergeBy(SyncEntity.WATCHLIST_MOVIE, { it.tmdbId.toString() }, { it.addedAt.toEpochMillis() }, ::survives),
        collectionHidden =
          states
            .flatMap { it.movies.collectionHidden }
            .mergeBy(SyncEntity.HIDDEN_MOVIE, { it.tmdbId.toString() }, { it.addedAt.toEpochMillis() }, ::survives),
        progressPinned =
          states
            .flatMap { it.movies.progressPinned }
            .mergeBy(SyncEntity.PINNED_MOVIE, { it.toString() }, { 0 }, ::survives),
        ratingsMovies =
          states
            .flatMap { it.movies.ratingsMovies }
            .mergeBy(SyncEntity.MOVIE_RATING, { it.tmdbId.toString() }, { it.ratedAt.toEpochMillis() }, ::survives),
      )

    val lists =
      states
        .flatMap { it.lists.lists }
        .mergeBy(SyncEntity.CUSTOM_LIST, { it.id.toString() }, { it.updatedAt.toEpochMillis() }, ::survives)
        .map { list -> list.withMergedItems(states, ::survives) }

    return Result(
      state = local.copy(shows = shows, movies = movies, lists = BackupLists(lists = lists)),
      tombstones =
        tombstones.map { (key, deletedAt) ->
          SyncTombstoneEntry(entity = key.first, key = key.second, deletedAt = deletedAt)
        },
    )
  }

  /**
   * A list's items live inside it, so they are gathered from every device's copy of that same list rather than from the one copy that happened to win.
   * Otherwise an item added on another device would be dropped along with the losing copy of its list.
   */
  private fun BackupList.withMergedItems(
    states: List<BackupScheme>,
    survives: (SyncEntity, String) -> Boolean,
  ): BackupList {
    val items =
      states
        .flatMap { state -> state.lists.lists.filter { it.id == id } }
        .flatMap { it.items }
        .mergeBy(
          SyncEntity.CUSTOM_LIST_ITEM,
          { SyncEntity.CUSTOM_LIST_ITEM.key(id, it.type, it.tmdbId) },
          { it.updatedAt.toEpochMillis() },
          survives,
        )
    return copy(items = items, itemCount = items.size.toLong())
  }

  /**
   * Unions entries from every device, keeps the newest copy of each, and drops whatever a newer tombstone has deleted.
   */
  private fun <T> List<T>.mergeBy(
    entity: SyncEntity,
    key: (T) -> String,
    timestamp: (T) -> Long,
    survives: (SyncEntity, String) -> Boolean,
  ): List<T> {
    val newest = LinkedHashMap<String, T>()
    forEach { candidate ->
      val id = key(candidate)
      val existing = newest[id]
      if (existing == null || timestamp(candidate) > timestamp(existing)) {
        newest[id] = candidate
      }
    }
    return newest
      .filterKeys { survives(entity, it) }
      .values
      .toList()
  }

  private fun newestPerKey(entries: List<SyncTombstoneEntry>): Map<Pair<SyncEntity, String>, Long> {
    val newest = mutableMapOf<Pair<SyncEntity, String>, Long>()
    entries.forEach { entry ->
      val id = entry.entity to entry.key
      val existing = newest[id]
      if (existing == null || entry.deletedAt > existing) newest[id] = entry.deletedAt
    }
    return newest
  }
}
