package xyz.stignarnia.uiWidgets.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import xyz.stignarnia.uiModel.WidgetAmoled
import xyz.stignarnia.uiModel.WidgetTheme

/**
 * The rules behind which palette a widget ends up in.
 *
 * Worth pinning because none of them can be seen from a screenshot of one device: they are about a widget that was never configured, a phone too old for the choice it was given, and a switch that only applies half the time.
 */
class WidgetPaletteChoiceTest {
  private fun choose(
    storedTheme: WidgetTheme = WidgetTheme.FOLLOW_APP,
    storedAmoled: WidgetAmoled = WidgetAmoled.FOLLOW_APP,
    appThemeId: String? = "DARK",
    appAmoled: Boolean = false,
    isSystemDark: Boolean = false,
    isDynamicSupported: Boolean = true,
  ) = WidgetPaletteChoice.of(
    storedTheme = storedTheme,
    storedAmoled = storedAmoled,
    appThemeId = appThemeId,
    appAmoled = appAmoled,
    isSystemDark = isSystemDark,
    isDynamicSupported = isDynamicSupported,
  )

  @Test
  fun `a widget that was never configured follows the app`() {
    assertThat(choose(appThemeId = "LIGHT")).isEqualTo(WidgetPaletteKind.LIGHT)
    assertThat(choose(appThemeId = "DARK")).isEqualTo(WidgetPaletteKind.DARK)
    assertThat(choose(appThemeId = "DYNAMIC_LIGHT")).isEqualTo(WidgetPaletteKind.DYNAMIC_LIGHT)
  }

  @Test
  fun `a widget that was configured keeps its own theme whatever the app is set to`() {
    assertThat(choose(storedTheme = WidgetTheme.LIGHT, appThemeId = "DARK")).isEqualTo(WidgetPaletteKind.LIGHT)
    assertThat(choose(storedTheme = WidgetTheme.DARK, appThemeId = "LIGHT")).isEqualTo(WidgetPaletteKind.DARK)
  }

  @Test
  fun `follow system reads the device, whether the widget or the app asked for it`() {
    assertThat(choose(appThemeId = "SYSTEM", isSystemDark = true)).isEqualTo(WidgetPaletteKind.DARK)
    assertThat(choose(appThemeId = "SYSTEM", isSystemDark = false)).isEqualTo(WidgetPaletteKind.LIGHT)
    assertThat(choose(storedTheme = WidgetTheme.SYSTEM, appThemeId = "LIGHT", isSystemDark = true))
      .isEqualTo(WidgetPaletteKind.DARK)
  }

  @Test
  fun `pure black only applies where the widget is dark`() {
    assertThat(choose(storedTheme = WidgetTheme.DARK, storedAmoled = WidgetAmoled.ON))
      .isEqualTo(WidgetPaletteKind.BLACK)
    assertThat(choose(storedTheme = WidgetTheme.LIGHT, storedAmoled = WidgetAmoled.ON))
      .isEqualTo(WidgetPaletteKind.LIGHT)
    assertThat(choose(storedTheme = WidgetTheme.SYSTEM, storedAmoled = WidgetAmoled.ON, isSystemDark = false))
      .isEqualTo(WidgetPaletteKind.LIGHT)
  }

  /** Following the app's theme means following its switch too, even over a value this widget was pinned to earlier. */
  @Test
  fun `a widget on the app's theme takes the app's pure black switch`() {
    assertThat(choose(storedAmoled = WidgetAmoled.OFF, appThemeId = "DARK", appAmoled = true))
      .isEqualTo(WidgetPaletteKind.BLACK)
    assertThat(choose(storedAmoled = WidgetAmoled.ON, appThemeId = "DARK", appAmoled = false))
      .isEqualTo(WidgetPaletteKind.DARK)
  }

  /** The third state is the point of storing the switch as an enum: a widget can hold the opposite answer to the app's. */
  @Test
  fun `the widget's own pure black switch wins over the app's`() {
    assertThat(choose(storedTheme = WidgetTheme.DARK, storedAmoled = WidgetAmoled.OFF, appAmoled = true))
      .isEqualTo(WidgetPaletteKind.DARK)
    assertThat(choose(storedTheme = WidgetTheme.DARK, storedAmoled = WidgetAmoled.ON, appAmoled = false))
      .isEqualTo(WidgetPaletteKind.BLACK)
    assertThat(choose(storedTheme = WidgetTheme.DARK, storedAmoled = WidgetAmoled.FOLLOW_APP, appAmoled = true))
      .isEqualTo(WidgetPaletteKind.BLACK)
  }

  @Test
  fun `material you is black under the pure black switch, not dark grey`() {
    assertThat(choose(storedTheme = WidgetTheme.DYNAMIC_DARK, storedAmoled = WidgetAmoled.ON))
      .isEqualTo(WidgetPaletteKind.DYNAMIC_BLACK)
  }

  /**
   * Without Material You a dynamic choice keeps its brightness and loses the wallpaper - the same fallback AppTheme.fromId makes for the app.
   * It applies to a stored choice as much as to the app's: the picker offers no dynamic entry on such a device, so one can only have arrived in a backup from a newer phone.
   */
  @Test
  fun `without material you a dynamic theme keeps its brightness`() {
    assertThat(choose(appThemeId = "DYNAMIC_LIGHT", isDynamicSupported = false))
      .isEqualTo(WidgetPaletteKind.LIGHT)
    assertThat(choose(appThemeId = "DYNAMIC_DARK", isDynamicSupported = false))
      .isEqualTo(WidgetPaletteKind.DARK)
    assertThat(choose(appThemeId = "DYNAMIC_SYSTEM", isSystemDark = true, isDynamicSupported = false))
      .isEqualTo(WidgetPaletteKind.DARK)
    assertThat(choose(storedTheme = WidgetTheme.DYNAMIC_LIGHT, isDynamicSupported = false))
      .isEqualTo(WidgetPaletteKind.LIGHT)
  }

  /** Losing the wallpaper does not lose the rest of the choice: a stored theme and switch still apply. */
  @Test
  fun `without material you a stored choice is still the widget's own`() {
    assertThat(
      choose(
        storedTheme = WidgetTheme.DYNAMIC_DARK,
        storedAmoled = WidgetAmoled.ON,
        appThemeId = "LIGHT",
        isDynamicSupported = false,
      ),
    ).isEqualTo(WidgetPaletteKind.BLACK)
  }

  @Test
  fun `an unknown stored theme falls to dark, as the app does`() {
    assertThat(choose(appThemeId = null)).isEqualTo(WidgetPaletteKind.DARK)
    assertThat(choose(appThemeId = "SOMETHING_ELSE")).isEqualTo(WidgetPaletteKind.DARK)
  }
}
