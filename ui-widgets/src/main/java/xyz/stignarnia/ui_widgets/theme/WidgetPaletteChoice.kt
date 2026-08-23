package xyz.stignarnia.ui_widgets.theme

import xyz.stignarnia.ui_model.WidgetAmoled
import xyz.stignarnia.ui_model.WidgetTheme

/** The palettes a widget can end up in, once every choice has been made. */
internal enum class WidgetPaletteKind {
  LIGHT,
  DARK,
  BLACK,
  DYNAMIC_LIGHT,
  DYNAMIC_DARK,
  DYNAMIC_BLACK,
}

/**
 * Which palette a widget is drawn in, decided without touching Android.
 *
 * Split out from [WidgetPalettes] so the rules can be tested: they are where this feature is most likely to be wrong, and every one of them is a question about state rather than about colour.
 */
internal object WidgetPaletteChoice {

  /**
   * Only reached where a widget can be themed at all, which is API 31 - see [WidgetPalettes] - so the widget's own stored choice is honoured whenever this runs.
   *
   * @param storedTheme what the widget's own settings screen last stored.
   * @param storedAmoled the same for the pure black switch.
   * @param appThemeId the app's theme, as AppTheme persists it - the answer for a widget that was never configured.
   * @param appAmoled the app's pure black switch, the same way.
   * @param isSystemDark whether the device is dark right now, which is the only thing "follow system" can mean here.
   * @param isDynamicSupported whether Material You can be honoured, which needs the API 34 surface roles. The picker leaves those entries out below 34, so a dynamic choice arriving here from a device that had them is a backup restored from a newer phone.
   */
  fun of(
    storedTheme: WidgetTheme,
    storedAmoled: WidgetAmoled,
    appThemeId: String?,
    appAmoled: Boolean,
    isSystemDark: Boolean,
    isDynamicSupported: Boolean,
  ): WidgetPaletteKind {
    // FOLLOW_APP is not a theme, it is a pointer at the app's, and the ids the two enums store are deliberately the same strings.
    // An id this build does not know falls to DARK, as AppTheme.fromId does.
    val theme = if (storedTheme == WidgetTheme.FOLLOW_APP) {
      WidgetTheme.fromName(appThemeId).takeIf { it != WidgetTheme.FOLLOW_APP } ?: WidgetTheme.DARK
    } else {
      storedTheme
    }

    val isDynamic = theme.isDynamic && isDynamicSupported
    val isDark = when (theme) {
      WidgetTheme.LIGHT, WidgetTheme.DYNAMIC_LIGHT -> false
      WidgetTheme.DARK, WidgetTheme.DYNAMIC_DARK -> true
      else -> isSystemDark
    }

    // Following the app means following all of it: a widget set to the app's theme takes the app's pure black switch too, whatever it was last told on its own.
    // Otherwise "same as app" would be a half truth - the right theme with someone else's switch on top - and the widget could sit black beside a plain dark app.
    val amoled = if (storedTheme == WidgetTheme.FOLLOW_APP) {
      appAmoled
    } else {
      when (storedAmoled) {
        WidgetAmoled.ON -> true
        WidgetAmoled.OFF -> false
        WidgetAmoled.FOLLOW_APP -> appAmoled
      }
    }
    // The switch only has something to act on where the widget is dark, exactly as in the app.
    val isBlack = amoled && isDark

    return when {
      isDynamic && isBlack -> WidgetPaletteKind.DYNAMIC_BLACK
      isDynamic && isDark -> WidgetPaletteKind.DYNAMIC_DARK
      isDynamic -> WidgetPaletteKind.DYNAMIC_LIGHT
      isBlack -> WidgetPaletteKind.BLACK
      isDark -> WidgetPaletteKind.DARK
      else -> WidgetPaletteKind.LIGHT
    }
  }

  private val WidgetTheme.isDynamic: Boolean
    get() = this == WidgetTheme.DYNAMIC_SYSTEM || this == WidgetTheme.DYNAMIC_LIGHT || this == WidgetTheme.DYNAMIC_DARK
}
