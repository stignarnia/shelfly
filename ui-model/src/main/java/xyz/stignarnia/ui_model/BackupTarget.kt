package xyz.stignarnia.ui_model

/**
 * Where scheduled backups are written.
 *
 * Both targets run the same pipeline - build the JSON, write it, read it back
 * to verify, prune to the newest few - and differ only in how those four steps
 * reach storage.
 */
enum class BackupTarget {
  /** A folder the user picked through the system file picker. */
  LOCAL_FOLDER,

  /** A directory on the user's own WebDAV server. */
  WEBDAV,
}
