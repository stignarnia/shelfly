package xyz.stignarnia.uiBackup.features.imports.model

/**
 * The state of listing backups held on the WebDAV server, before one is chosen.
 */
sealed interface WebDavBackups {
  data object Idle : WebDavBackups

  data object Loading : WebDavBackups

  /**
   * File names, newest first.
   * Empty when the server has no backups yet.
   */
  data class Loaded(
    val fileNames: List<String>,
  ) : WebDavBackups
}
