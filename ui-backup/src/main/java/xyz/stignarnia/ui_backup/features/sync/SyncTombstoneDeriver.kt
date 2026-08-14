package xyz.stignarnia.ui_backup.features.sync

import xyz.stignarnia.ui_backup.features.sync.model.SyncTombstoneEntry
import xyz.stignarnia.ui_backup.model.BackupScheme

/**
 * Works out what this device deleted, by comparing local state against the
 * state it last published.
 *
 * The alternative was recording every deletion as it happens, at every call
 * site that removes something. That is around sixty places once the hidden
 * ones are counted - moving a show into the collection quietly removes it from
 * the watchlist and from hidden - and each addition would have to clear its
 * tombstone again. One missed site is a deletion that never propagates, which
 * surfaces months later as a show that keeps coming back.
 *
 * Diffing cannot miss a site, because it does not know about sites at all.
 */
internal object SyncTombstoneDeriver {

  /**
   * @param published what this device last uploaded, or null if it has never
   *   synced - in which case nothing is treated as deleted, so a first sync
   *   only ever adds.
   * @param local current state.
   * @param deletedAt when to date the deletions. Pass the *previous* sync time,
   *   not now: all that is known is that the deletion happened somewhere in
   *   between, and taking the earlier bound lets a genuine re-add on another
   *   device win the tie. Preserving data is the better way to be wrong.
   */
  fun derive(
    published: BackupScheme?,
    local: BackupScheme,
    deletedAt: Long,
  ): List<SyncTombstoneEntry> {
    if (published == null) return emptyList()

    val before = SyncStateFlattener.flatten(published)
    val now = SyncStateFlattener.flatten(local)

    return before.keys
      .subtract(now.keys)
      .map { (entity, key) ->
        SyncTombstoneEntry(entity = entity, key = key, deletedAt = deletedAt)
      }
  }
}
