package xyz.stignarnia.data_webdav

/**
 * The slice of WebDAV a backup target needs: write a file, read it back, list
 * the directory, delete what has aged out.
 *
 * Every call returns [Result]. Failures are always a [WebDavError] so callers
 * can tell a wrong password from an unreachable host without inspecting
 * exception types from OkHttp.
 */
interface WebDavClient {

  /**
   * Verifies the directory is reachable and writable, creating it if the parent
   * allows. Used by the "Test connection" action.
   */
  suspend fun testConnection(credentials: WebDavCredentials): Result<Unit>

  suspend fun put(
    credentials: WebDavCredentials,
    fileName: String,
    content: String,
  ): Result<Unit>

  suspend fun get(
    credentials: WebDavCredentials,
    fileName: String,
  ): Result<String>

  /**
   * Lists the files directly inside the directory. Collections are excluded, so
   * the result is files only.
   */
  suspend fun list(credentials: WebDavCredentials): Result<List<WebDavFile>>

  suspend fun delete(
    credentials: WebDavCredentials,
    fileName: String,
  ): Result<Unit>
}

/**
 * A file entry from a PROPFIND listing.
 *
 * [lastModifiedMillis] is 0 when the server omits `getlastmodified`, which is
 * legal; callers sorting by age should fall back to the name, which carries a
 * timestamp.
 */
data class WebDavFile(
  val name: String,
  val lastModifiedMillis: Long,
)
