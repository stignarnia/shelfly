package xyz.stignarnia.data_webdav

/**
 * Why a WebDAV call failed, in the terms a user can act on.
 *
 * The distinction matters for the "Test connection" action: a wrong password, a
 * mistyped path and a self-signed certificate all look like "it didn't work"
 * otherwise, and they need completely different fixes.
 */
sealed class WebDavError(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {

  /** 401 or 403 - the username or password is wrong, or the user lacks access. */
  data object Unauthorized : WebDavError("Authentication failed.") {
    private fun readResolve(): Any = Unauthorized
  }

  /** 404 - the directory does not exist at that URL. */
  data object NotFound : WebDavError("Directory not found.") {
    private fun readResolve(): Any = NotFound
  }

  /** TLS could not be negotiated, typically a self-signed or expired certificate. */
  class TlsFailure(
    cause: Throwable,
  ) : WebDavError("Could not establish a secure connection.", cause)

  /** The server was unreachable: no route, DNS failure, timeout. */
  class Unreachable(
    cause: Throwable,
  ) : WebDavError("Could not reach the server.", cause)

  /** The server answered, but with something unexpected. */
  class UnexpectedResponse(
    val code: Int,
  ) : WebDavError("Unexpected server response ($code).")
}
