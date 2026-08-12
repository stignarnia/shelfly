package xyz.stignarnia.data_webdav

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/**
 * Hand-rolled WebDAV over OkHttp.
 *
 * WebDAV is HTTP plus a handful of extra verbs, and the app only needs five of
 * them, so a Retrofit interface would mostly be fighting the XML body of
 * PROPFIND. This is small enough to read in one sitting and testable against
 * MockWebServer.
 */
@Singleton
internal class OkHttpWebDavClient @Inject constructor(
  private val okHttpClient: OkHttpClient,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WebDavClient {

  companion object {
    private const val PROPFIND = "PROPFIND"
    private const val MKCOL = "MKCOL"
    private val JSON = "application/json".toMediaType()
    private val XML = "application/xml; charset=utf-8".toMediaType()

    /** Ask only for what the retention rule uses: the name and the mtime. */
    private val PROPFIND_BODY =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <d:propfind xmlns:d="DAV:">
        <d:prop>
          <d:displayname/>
          <d:getlastmodified/>
          <d:resourcetype/>
        </d:prop>
      </d:propfind>
      """.trimIndent()
  }

  override suspend fun testConnection(credentials: WebDavCredentials): Result<Unit> =
    execute(credentials) {
      val listing = request(credentials, credentials.normalizedUrl)
        .method(PROPFIND, PROPFIND_BODY.toRequestBody(XML))
        .header("Depth", "0")
        .build()

      okHttpClient.newCall(listing).execute().use { response ->
        when {
          response.isSuccessful || response.code == 207 -> Unit
          // A missing directory is recoverable when the parent is writable, so
          // try to create it rather than reporting a dead end.
          response.code == 404 -> createDirectory(credentials)
          else -> throw response.toError()
        }
      }
    }

  override suspend fun put(
    credentials: WebDavCredentials,
    fileName: String,
    content: String,
  ): Result<Unit> =
    execute(credentials) {
      val request = request(credentials, credentials.normalizedUrl + fileName)
        .put(content.toRequestBody(JSON))
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw response.toError()
      }
    }

  override suspend fun get(
    credentials: WebDavCredentials,
    fileName: String,
  ): Result<String> =
    execute(credentials) {
      val request = request(credentials, credentials.normalizedUrl + fileName)
        .get()
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw response.toError()
        response.body?.string() ?: throw WebDavError.UnexpectedResponse(response.code)
      }
    }

  override suspend fun list(credentials: WebDavCredentials): Result<List<WebDavFile>> =
    execute(credentials) {
      val request = request(credentials, credentials.normalizedUrl)
        .method(PROPFIND, PROPFIND_BODY.toRequestBody(XML))
        .header("Depth", "1")
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful && response.code != 207) throw response.toError()
        val body = response.body?.string().orEmpty()
        WebDavResponseParser.parseFileListing(body)
      }
    }

  override suspend fun delete(
    credentials: WebDavCredentials,
    fileName: String,
  ): Result<Unit> =
    execute(credentials) {
      val request = request(credentials, credentials.normalizedUrl + fileName)
        .delete()
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        // 404 means it is already gone, which is the outcome we wanted.
        if (!response.isSuccessful && response.code != 404) throw response.toError()
      }
    }

  private fun createDirectory(credentials: WebDavCredentials) {
    val request = request(credentials, credentials.normalizedUrl)
      .method(MKCOL, EMPTY_BODY)
      .build()

    okHttpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw response.toError()
    }
  }

  private fun request(
    credentials: WebDavCredentials,
    url: String,
  ) = Request
    .Builder()
    .url(url)
    .header("Authorization", Credentials.basic(credentials.username, credentials.password))

  /**
   * Runs a blocking OkHttp call off the main thread and maps anything thrown
   * onto [WebDavError], so callers never see a raw OkHttp or TLS exception.
   */
  private suspend fun <T> execute(
    credentials: WebDavCredentials,
    block: () -> T,
  ): Result<T> =
    withContext(dispatcher) {
      if (!credentials.isComplete) {
        return@withContext Result.failure(WebDavError.NotFound)
      }
      try {
        Result.success(block())
      } catch (error: WebDavError) {
        Timber.w("WebDAV call failed: ${error.message}")
        Result.failure(error)
      } catch (error: SSLException) {
        Timber.w(error, "WebDAV TLS failure")
        Result.failure(WebDavError.TlsFailure(error))
      } catch (error: IOException) {
        Timber.w(error, "WebDAV unreachable")
        Result.failure(WebDavError.Unreachable(error))
      } catch (error: IllegalArgumentException) {
        // OkHttp rejects a malformed URL here rather than at request time.
        Timber.w(error, "WebDAV URL malformed")
        Result.failure(WebDavError.Unreachable(error))
      }
    }

  private fun Response.toError(): WebDavError =
    when (code) {
      401, 403 -> WebDavError.Unauthorized
      404 -> WebDavError.NotFound
      else -> WebDavError.UnexpectedResponse(code)
    }
}

private val EMPTY_BODY: RequestBody = ByteArray(0).toRequestBody(null)
