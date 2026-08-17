package xyz.stignarnia.ui_backup.features.sync

/**
 * The stages of one backup-and-sync run, in the order they happen.
 * [percent] is how much of the run is done once that stage has finished, so reporting a stage means "this much is behind us".
 *
 * The weights are spread by what each stage costs rather than evenly, because an even split would crawl through the four cheap local steps and then stall for most of the run on [CHANGES_APPLIED].
 * The expensive ones are the round trips to the server and the import, which fetches details for everything a peer knows about and this device does not.
 *
 * They are still estimates: a stage cannot report a fraction of itself, so the ring moves in steps, and [PEERS_DOWNLOADED] costs whatever the slowest peer costs.
 * What is guaranteed is the ordering and that 100 means finished - the ring never sits full while work is left, and never rewinds.
 */
enum class BackupSyncStage(
  val percent: Int,
) {
  /** The configured target was read and is usable. */
  DESTINATION_RESOLVED(2),

  /** The snapshot was built from the database and written to the target. */
  SNAPSHOT_WRITTEN(15),

  /** The snapshot was read back and parsed, proving it landed intact. */
  SNAPSHOT_VERIFIED(20),

  /** Backups beyond the configured retention were deleted. */
  OLD_BACKUPS_PRUNED(24),

  /** This device's own state was collected for the sync cycle. */
  LOCAL_STATE_READ(32),

  /** What this device last published came back down, giving the diff baseline. */
  OWN_STATE_DOWNLOADED(38),

  /** Every peer's published state came down. */
  PEERS_DOWNLOADED(52),

  /** Local state, tombstones and peers were merged into one result. */
  MERGED(56),

  /** The merge result was applied locally: removals, then the import. */
  CHANGES_APPLIED(84),

  /** What this device ended up with was published back to the server. */
  PUBLISHED(100),
}
