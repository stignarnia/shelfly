package xyz.stignarnia.dataWebdav

/**
 * Where backups go and how to authenticate.
 *
 * [url] points at the *directory* backups live in, not a file.
 * A trailing slash is optional; [normalizedUrl] adds one, since WebDAV collections require it and servers differ on how forgiving they are about its absence.
 */
data class WebDavCredentials(
  val url: String,
  val username: String,
  val password: String,
) {
  val normalizedUrl: String
    get() = url.trim().let { if (it.endsWith("/")) it else "$it/" }

  /**
   * Anonymous WebDAV exists, so a blank password is allowed; a blank URL is not.
   */
  val isComplete: Boolean
    get() = url.isNotBlank()

  companion object {
    val EMPTY = WebDavCredentials(url = "", username = "", password = "")
  }
}
