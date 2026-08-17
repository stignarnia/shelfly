package xyz.stignarnia.ui_backup.features.export.targets

import xyz.stignarnia.data_webdav.WebDavClient
import xyz.stignarnia.data_webdav.WebDavCredentials

/**
 * Writes into a directory on the user's own WebDAV server.
 *
 * Thinner than the local target: WebDAV addresses files by name under the configured directory, so there is no handle to keep or document to create ahead of the write.
 */
internal class WebDavBackupDestination(
  private val webDavClient: WebDavClient,
  private val credentials: WebDavCredentials,
) : BackupDestination {

  override suspend fun write(
    fileName: String,
    content: String,
  ): Result<Unit> = webDavClient.put(credentials, fileName, content)

  override suspend fun read(fileName: String): Result<String> = webDavClient.get(credentials, fileName)

  override suspend fun list(): Result<List<BackupEntry>> =
    webDavClient
      .list(credentials)
      .map { files ->
        files.map { BackupEntry(name = it.name, lastModifiedMillis = it.lastModifiedMillis) }
      }

  override suspend fun delete(entry: BackupEntry): Result<Unit> = webDavClient.delete(credentials, entry.name)
}
