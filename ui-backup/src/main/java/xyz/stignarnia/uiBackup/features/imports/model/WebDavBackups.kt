package xyz.stignarnia.uiBackup.features.imports.model

/**
 * The state of listing backups held on the WebDAV server, before one is chosen.
 */
sealed interface WebDavBackups {
  data object Idle : WebDavBackups

  data object Loading : WebDavBackups

  /**
   * Backups, newest first.
   * Empty when the server has no backups yet.
   */
  data class Loaded(
    val backups: List<WebDavBackup>,
  ) : WebDavBackups
}

/**
 * A backup on the server.
 * [label] is what the user picks from, [fileName] is what gets downloaded.
 */
data class WebDavBackup(
  val fileName: String,
  val label: String,
)
