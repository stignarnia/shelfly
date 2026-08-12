package xyz.stignarnia.data_webdav

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class OkHttpWebDavClientTest {

  private lateinit var server: MockWebServer
  private lateinit var SUT: OkHttpWebDavClient

  private fun credentials() =
    WebDavCredentials(
      url = server.url("/backups").toString(),
      username = "user",
      password = "secret",
    )

  @Before
  fun setUp() {
    server = MockWebServer().apply { start() }
    SUT = OkHttpWebDavClient(OkHttpClient(), Dispatchers.Unconfined)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `Should PUT the backup to the directory with basic auth`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(201))

      val result = SUT.put(credentials(), "shelfly_export_1.json", """{"version":3}""")

      assertThat(result.isSuccess).isTrue()
      val request = server.takeRequest()
      assertThat(request.method).isEqualTo("PUT")
      assertThat(request.path).isEqualTo("/backups/shelfly_export_1.json")
      assertThat(request.getHeader("Authorization")).startsWith("Basic ")
      assertThat(request.body.readUtf8()).isEqualTo("""{"version":3}""")
    }

  @Test
  fun `Should append a trailing slash to a directory url that lacks one`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(201))

      val noSlash = credentials().copy(url = server.url("/backups").toString())
      SUT.put(noSlash, "file.json", "{}")

      assertThat(server.takeRequest().path).isEqualTo("/backups/file.json")
    }

  @Test
  fun `Should read a backup back`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(200).setBody("""{"version":3}"""))

      val result = SUT.get(credentials(), "shelfly_export_1.json")

      assertThat(result.getOrNull()).isEqualTo("""{"version":3}""")
      assertThat(server.takeRequest().method).isEqualTo("GET")
    }

  @Test
  fun `Should report a wrong password as Unauthorized rather than a generic failure`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(401))

      val result = SUT.testConnection(credentials())

      assertThat(result.exceptionOrNull()).isInstanceOf(WebDavError.Unauthorized::class.java)
    }

  @Test
  fun `Should report a forbidden response as Unauthorized too`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(403))

      val result = SUT.get(credentials(), "file.json")

      assertThat(result.exceptionOrNull()).isInstanceOf(WebDavError.Unauthorized::class.java)
    }

  @Test
  fun `Should create the directory when the test finds it missing`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(404))
      server.enqueue(MockResponse().setResponseCode(201))

      val result = SUT.testConnection(credentials())

      assertThat(result.isSuccess).isTrue()
      assertThat(server.takeRequest().method).isEqualTo("PROPFIND")
      assertThat(server.takeRequest().method).isEqualTo("MKCOL")
    }

  @Test
  fun `Should surface a directory that cannot be created as NotFound`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(404))
      server.enqueue(MockResponse().setResponseCode(404))

      val result = SUT.testConnection(credentials())

      assertThat(result.exceptionOrNull()).isInstanceOf(WebDavError.NotFound::class.java)
    }

  @Test
  fun `Should report an unreachable server rather than throwing`() =
    runTest {
      server.shutdown()

      val result = SUT.testConnection(credentials())

      assertThat(result.exceptionOrNull()).isInstanceOf(WebDavError.Unreachable::class.java)
    }

  @Test
  fun `Should reject an incomplete configuration without a network call`() =
    runTest {
      val result = SUT.testConnection(WebDavCredentials.EMPTY)

      assertThat(result.isFailure).isTrue()
      assertThat(server.requestCount).isEqualTo(0)
    }

  @Test
  fun `Should treat a delete of something already gone as success`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(404))

      val result = SUT.delete(credentials(), "gone.json")

      assertThat(result.isSuccess).isTrue()
    }

  @Test
  fun `Should list files and skip the directory itself`() =
    runTest {
      server.enqueue(
        MockResponse()
          .setResponseCode(207)
          .setBody(MULTISTATUS),
      )

      val files = SUT.list(credentials()).getOrThrow()

      assertThat(files.map { it.name })
        .containsExactly("shelfly_export_20260101000000.json", "shelfly_export_20260102000000.json")
      val request = server.takeRequest()
      assertThat(request.method).isEqualTo("PROPFIND")
      assertThat(request.getHeader("Depth")).isEqualTo("1")
    }

  @Test
  fun `Should parse last modified so retention can sort by age`() =
    runTest {
      server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS))

      val files = SUT.list(credentials()).getOrThrow()

      val first = files.first { it.name == "shelfly_export_20260101000000.json" }
      val second = files.first { it.name == "shelfly_export_20260102000000.json" }
      assertThat(first.lastModifiedMillis).isGreaterThan(0)
      assertThat(second.lastModifiedMillis).isGreaterThan(first.lastModifiedMillis)
    }
}

/** Nextcloud-shaped response: uppercase "D:" prefix, directory listed first. */
private val MULTISTATUS = """
  <?xml version="1.0"?>
  <D:multistatus xmlns:D="DAV:">
    <D:response>
      <D:href>/backups/</D:href>
      <D:propstat>
        <D:prop>
          <D:resourcetype><D:collection/></D:resourcetype>
        </D:prop>
        <D:status>HTTP/1.1 200 OK</D:status>
      </D:propstat>
    </D:response>
    <D:response>
      <D:href>/backups/shelfly_export_20260101000000.json</D:href>
      <D:propstat>
        <D:prop>
          <D:getlastmodified>Thu, 01 Jan 2026 00:00:00 GMT</D:getlastmodified>
          <D:resourcetype/>
        </D:prop>
        <D:status>HTTP/1.1 200 OK</D:status>
      </D:propstat>
    </D:response>
    <D:response>
      <D:href>/backups/shelfly_export_20260102000000.json</D:href>
      <D:propstat>
        <D:prop>
          <D:getlastmodified>Fri, 02 Jan 2026 00:00:00 GMT</D:getlastmodified>
          <D:resourcetype/>
        </D:prop>
        <D:status>HTTP/1.1 200 OK</D:status>
      </D:propstat>
    </D:response>
  </D:multistatus>
""".trimIndent()
