package xyz.stignarnia.uiWidgets.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Holds the widget palettes to the app's own theme.
 *
 * The widgets cannot read a theme - their colours are resolved in Kotlin and pushed into the launcher - so colors_widget.xml restates values that already exist in ui-base.
 * Restating them is the only way a widget can be light while the system is dark, and it is also how the two quietly drift apart: nothing fails to build when a theme colour is changed in one place and not the other, and the result is a widget that no longer matches the app beside it on the same home screen.
 *
 * The same guard, for the same reason, as ThemeResourcesTest in ui-base.
 */
class WidgetPaletteResourcesTest {
  private val widgetRes: File by lazy { locate("src/main/res", "ui-widgets/src/main/res") }
  private val baseRes: File by lazy { locate("../ui-base/src/main/res", "ui-base/src/main/res") }

  /** Run from the module or from the root, depending on what asked for the test. */
  private fun locate(vararg candidates: String): File =
    candidates
      .map(::File)
      .firstOrNull { it.isDirectory }
      ?: error("Cannot locate ${candidates.first()} from ${File(".").absolutePath}")

  private fun colors(
    root: File,
    path: String,
  ): Map<String, String> {
    val text =
      root
        .resolve(path)
        .also { assertThat(it.exists()).isTrue() }
        .readText()
        .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    return Regex("<color name=\"([^\"]+)\">([^<]+)</color>")
      .findAll(text)
      .associate { it.groupValues[1] to it.groupValues[2].trim().uppercase() }
  }

  private val widgetColors by lazy { colors(widgetRes, "values/colors_widget.xml") }
  private val lightTheme by lazy { colors(baseRes, "values/colors_theme.xml") }
  private val darkTheme by lazy { colors(baseRes, "values-night/colors_theme.xml") }
  private val blackTheme by lazy { colors(baseRes, "values-night/colors_amoled.xml") }

  /** The roles every palette has to answer, since WidgetPalette reads all of them for each one. */
  private val roles =
    listOf(
      "Background",
      "TextPrimary",
      "TextSecondary",
      "Accent",
      "AccentText",
      "Badge",
      "PlaceholderInk",
      "SearchBackground",
      "StatusBackground",
      "StatusText",
    )

  @Test
  fun `every palette answers every role`() {
    listOf("widgetLight", "widgetDark", "widgetBlack").forEach { palette ->
      roles.forEach { role ->
        assertThat(widgetColors).containsKey("$palette$role")
      }
    }
    assertThat(widgetColors).hasSize(roles.size * 3)
  }

  @Test
  fun `the light palette matches the app's light theme`() {
    assertMatches(
      "widgetLight",
      lightTheme,
      mapOf(
        "Background" to "themeBackground",
        "TextPrimary" to "themeTextPrimary",
        "TextSecondary" to "themeTextSecondary",
        "Accent" to "themeAccent",
        "AccentText" to "themeAccentText",
        "Badge" to "themeElevated",
        "PlaceholderInk" to "themePlaceholderInk",
        "SearchBackground" to "themeSurface",
      ),
    )
  }

  @Test
  fun `the dark palette matches the app's dark theme`() {
    assertMatches(
      "widgetDark",
      darkTheme,
      mapOf(
        "Background" to "themeBackground",
        "TextPrimary" to "themeTextPrimary",
        "TextSecondary" to "themeTextSecondary",
        "Accent" to "themeAccent",
        "AccentText" to "themeAccentText",
        "Badge" to "themeElevated",
        "PlaceholderInk" to "themePlaceholderInk",
        "SearchBackground" to "themeSurface",
      ),
    )
  }

  /**
   * The black palette is the dark one with the AMOLED overlay's surfaces over it, which is what applying that overlay does to the app.
   * Only the surfaces move: text, accent and placeholder ink are the dark theme's, and are checked against it.
   */
  @Test
  fun `the black palette matches the amoled overlay over the dark theme`() {
    assertMatches(
      "widgetBlack",
      blackTheme,
      mapOf(
        "Background" to "amoledBackground",
        "Badge" to "amoledElevated",
        "SearchBackground" to "amoledSurface",
      ),
    )
    assertMatches(
      "widgetBlack",
      darkTheme,
      mapOf(
        "TextPrimary" to "themeTextPrimary",
        "TextSecondary" to "themeTextSecondary",
        "Accent" to "themeAccent",
        "AccentText" to "themeAccentText",
        "PlaceholderInk" to "themePlaceholderInk",
      ),
    )
  }

  private fun assertMatches(
    palette: String,
    theme: Map<String, String>,
    equivalents: Map<String, String>,
  ) = equivalents.forEach { (role, themeName) ->
    assertThat(widgetColors.getValue("$palette$role")).isEqualTo(theme.getValue(themeName))
  }
}
