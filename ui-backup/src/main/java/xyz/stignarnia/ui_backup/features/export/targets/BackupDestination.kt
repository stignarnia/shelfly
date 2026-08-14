package xyz.stignarnia.ui_backup.features.export.targets

/**
 * The storage a scheduled backup writes to.
 *
 * Both destinations run the same pipeline - write, read back to verify, list,
 * prune - so the worker holds the policy and the destination only knows how to
 * reach its storage. That is the whole reason WebDAV is a second target rather
 * than a second backup system.
 */
internal interface BackupDestination {

  suspend fun write(
    fileName: String,
    content: String,
  ): Result<Unit>

  /**
   * Reads a file straight back after writing it. The scheduled backup verifies
   * every write this way, so a destination that silently accepts and discards
   * is caught rather than reported as a success.
   */
  suspend fun read(fileName: String): Result<String>

  suspend fun list(): Result<List<BackupEntry>>

  suspend fun delete(entry: BackupEntry): Result<Unit>
}

/**
 * A backup already sitting at the destination.
 *
 * [lastModifiedMillis] can be 0 when the storage does not report one. Retention
 * falls back to the timestamp in the file name in that case, which is why the
 * name is part of the sort key too.
 */
internal data class BackupEntry(
  val name: String,
  val lastModifiedMillis: Long,
  val handle: Any? = null,
)
