package xyz.stignarnia.ui_base

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Guards the invariants the theme system rests on, none of which the compiler can see.
 *
 * A theme here is one style resolved against two sets of values, plus two overlays composed on top.
 * That makes the dangerous failure a *missing* entry rather than a wrong one: a colour defined for light but not dark, or an attribute the Material You overlay forgets, silently falls through to the static theme in one mode only.
 * Nothing fails to build, nothing throws, and it shows up as one wrong colour on one screen in one of six themes.
 *
 * These checks caught colorPrimaryDark missing from both overlays when they were written.
 */
class ThemeResourcesTest {

  private val res: File by lazy {
    listOf(File("src/main/res"), File("ui-base/src/main/res"))
      .firstOrNull { it.isDirectory }
      ?: error("Cannot locate ui-base res from ${File(".").absolutePath}")
  }

  /** Attributes deliberately pinned across every theme - see @style/AppTheme. */
  private val heldFixed = setOf(
    "textColorOnSurface",
    "textColorGridTitle",
    "colorErrorSnackbar",
    "textColorErrorSnackbar",
    "colorWidgetStatusBackground",
  )

  private val notAColour = setOf("switchStyle", "enableEdgeToEdge")

  private fun read(path: String) =
    res
      .resolve(path)
      .also { assertThat(it.exists()).isTrue() }
      .readText()
      .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

  private fun items(
    path: String,
    style: String? = null,
  ): Map<String, String> {
    var text = read(path)
    if (style != null) {
      text = Regex("<style name=\"${Regex.escape(style)}\".*?</style>", RegexOption.DOT_MATCHES_ALL)
        .find(text)
        ?.value
        ?: error("No <style name=\"$style\"> in $path")
    }
    return Regex("<item name=\"([^\"]+)\">([^<]+)</item>")
      .findAll(text)
      .associate { it.groupValues[1] to it.groupValues[2].trim() }
  }

  private fun colors(path: String) =
    Regex("<color name=\"([^\"]+)\">([^<]+)</color>")
      .findAll(read(path))
      .associate { it.groupValues[1] to it.groupValues[2].trim().uppercase() }

  @Test
  fun `themed colours are defined for both light and dark`() {
    val light = colors("values/colors_theme.xml")
    val dark = colors("values-night/colors_theme.xml")

    assertThat(light.keys).isEqualTo(dark.keys)
    assertThat(light).isNotEmpty()
  }

  @Test
  fun `amoled colours are defined for both light and dark`() {
    val light = colors("values/colors_amoled.xml")
    val dark = colors("values-night/colors_amoled.xml")

    assertThat(light.keys).isEqualTo(dark.keys)
    assertThat(light).isNotEmpty()
  }

  /**
   * The AMOLED overlay is applied whatever the night mode, so that the switch keeps working under "Follow system".
   * By day it must therefore resolve to exactly the light theme's own surfaces, or turning it on would change the light theme too.
   */
  @Test
  fun `amoled light values match the light theme surfaces`() {
    val amoled = colors("values/colors_amoled.xml")
    val theme = colors("values/colors_theme.xml")

    val equivalents = mapOf(
      "amoledBackground" to "themeBackground",
      "amoledSurface" to "themeSurface",
      "amoledCard" to "themeCard",
      "amoledElevated" to "themeElevated",
      "amoledPlaceholder" to "themePlaceholder",
      "amoledSearchStroke" to "themeSearchStroke",
    )

    assertThat(amoled.keys).isEqualTo(equivalents.keys)
    equivalents.forEach { (amoledName, themeName) ->
      assertThat(amoled.getValue(amoledName)).isEqualTo(theme.getValue(themeName))
    }
  }

  @Test
  fun `material you overlays set the same attributes in light and dark`() {
    val light = items("values-v34/themes_overlay.xml")
    val dark = items("values-night-v34/themes_overlay.xml")

    assertThat(light.keys).isEqualTo(dark.keys)
    assertThat(light).isNotEmpty()
  }

  /**
   * Anything AppTheme colours has to be answered by Material You, or that attribute keeps the static palette while everything around it follows the wallpaper.
   * Attributes whose value is already an attribute reference are excluded: overriding what they point at is enough.
   */
  @Test
  fun `material you covers every themed attribute of AppTheme`() {
    val appTheme = items("values/styles.xml", "AppTheme")
    val indirect = appTheme.filterValues { it.startsWith("?") }.keys
    val expected = appTheme.keys - heldFixed - notAColour - indirect

    assertThat(items("values-v34/themes_overlay.xml").keys)
      .containsAtLeastElementsIn(expected)
  }

  @Test
  fun `overlays never override an attribute held fixed across themes`() {
    listOf(
      "values/themes_overlay.xml",
      "values-v34/themes_overlay.xml",
      "values-night-v34/themes_overlay.xml",
    ).forEach { path ->
      assertThat(items(path).keys.intersect(heldFixed)).isEmpty()
    }
  }
}
