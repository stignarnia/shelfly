package xyz.stignarnia.data_webdav

import org.w3c.dom.Element
import org.xml.sax.InputSource
import timber.log.Timber
import java.io.StringReader
import java.net.URLDecoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the multistatus XML a PROPFIND answers with.
 *
 * Servers vary in namespace prefix ("d:", "D:", none at all), so lookups go by
 * local name rather than qualified name. Entries are matched on the href, which
 * is a URL path and therefore percent-encoded.
 */
internal object WebDavResponseParser {

  fun parseFileListing(xml: String): List<WebDavFile> {
    if (xml.isBlank()) return emptyList()

    return try {
      val document = DocumentBuilderFactory
        .newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(InputSource(StringReader(xml)))

      val responses = document.getElementsByTagNameNS("*", "response")
      (0 until responses.length)
        .mapNotNull { index -> responses.item(index) as? Element }
        .mapNotNull { it.toFile() }
    } catch (error: Exception) {
      Timber.w(error, "Could not parse WebDAV listing")
      emptyList()
    }
  }

  private fun Element.toFile(): WebDavFile? {
    // A <collection/> resourcetype marks a directory - including the one we
    // just listed, which every server returns as the first entry.
    if (firstChildText("resourcetype")?.isNotBlank() == true) return null
    if (getElementsByTagNameNS("*", "collection").length > 0) return null

    val href = firstChildText("href") ?: return null
    val name = href
      .trimEnd('/')
      .substringAfterLast('/')
      .let { decode(it) }
      .takeIf { it.isNotBlank() }
      ?: return null

    return WebDavFile(
      name = name,
      lastModifiedMillis = firstChildText("getlastmodified").toHttpDateMillis(),
    )
  }

  private fun Element.firstChildText(localName: String): String? {
    val nodes = getElementsByTagNameNS("*", localName)
    if (nodes.length == 0) return null
    return nodes.item(0).textContent?.trim()
  }

  private fun decode(value: String): String =
    try {
      URLDecoder.decode(value, Charsets.UTF_8.name())
    } catch (error: IllegalArgumentException) {
      value
    }

  /**
   * `getlastmodified` is an RFC 1123 date. Returns 0 when absent or unparseable,
   * which callers treat as "unknown age".
   */
  private fun String?.toHttpDateMillis(): Long {
    if (this.isNullOrBlank()) return 0
    return try {
      ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (error: Exception) {
      0
    }
  }
}
