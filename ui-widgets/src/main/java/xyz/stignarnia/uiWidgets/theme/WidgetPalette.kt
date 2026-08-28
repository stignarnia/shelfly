package xyz.stignarnia.uiWidgets.theme

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.utilities.AndroidVersion
import xyz.stignarnia.uiWidgets.R

/**
 * The colours one widget is drawn in.
 *
 * The app puts a theme on an Activity and lets the attributes resolve; a widget cannot.
 * Its layout is inflated by the launcher, in the launcher's process and against the launcher's configuration, and android:theme is fixed in the XML at build time - so a per widget choice can only reach the view tree as colours pushed onto the RemoteViews.
 * That is what this class is: the app's theme, already resolved, ready to be pushed.
 *
 * [mediaFrame] is a drawable rather than a colour because the poster frame carries two themed colours at once, a fill and a stroke, and a background tint would take both.
 */
data class WidgetPalette(
  @get:ColorInt val background: Int,
  @get:ColorInt val textPrimary: Int,
  @get:ColorInt val textSecondary: Int,
  @get:ColorInt val accent: Int,
  @get:ColorInt val accentText: Int,
  @get:ColorInt val badge: Int,
  @get:ColorInt val placeholderInk: Int,
  @get:ColorInt val searchBackground: Int,
  @get:ColorInt val statusBackground: Int,
  @get:ColorInt val statusText: Int,
  @get:DrawableRes val mediaFrame: Int,
)

/**
 * Works out which palette a widget is drawn in, from the widget's own choice and the app's settings.
 *
 * Two gates, each at the version where the thing it guards starts working:
 *
 * - Below API 31 there is no palette at all. [resolve] returns null and every caller leaves the widget alone, so it keeps inflating against @style/AppTheme.Widget.Dark exactly as it did before this existed. RemoteViews has no way to tint a background before then, and a widget half themed - themed text on an unthemed ground - is worse than one that is honestly dark. The configuration screen is gated at the same version, in res/xml-v31: where there is nothing to theme there is nothing to configure.
 * - Below API 34 there is no Material You, which needs the surface roles that arrive there - see AppTheme.isSupported. The picker leaves those entries out on such a device, and a dynamic choice restored from a newer phone keeps its brightness and loses the wallpaper.
 */
object WidgetPalettes {
  fun resolve(
    context: Context,
    widgetId: Int,
    settingsRepository: SettingsRepository,
  ): WidgetPalette {
    val storedTheme = settingsRepository.widgets.getWidgetTheme(widgetId)
    val storedAmoled = settingsRepository.widgets.getWidgetAmoled(widgetId)
    val storedTransparency = settingsRepository.widgets.getWidgetTransparency(widgetId)

    val kind =
      WidgetPaletteChoice.of(
        storedTheme = storedTheme,
        storedAmoled = storedAmoled,
        appThemeId = settingsRepository.themeId,
        appAmoled = settingsRepository.isAmoled,
        isSystemDark = context.isSystemDark(),
        isDynamicSupported = AndroidVersion.isAtLeastAndroid14,
      )

    val palette =
      when (kind) {
        WidgetPaletteKind.LIGHT -> light(context)

        WidgetPaletteKind.DARK -> dark(context)

        WidgetPaletteKind.BLACK -> black(context)

        // A dynamic kind is only ever chosen from API 34; the check restates that where lint can see it, and the fallback is the one the choice would have made without Material You.
        else -> if (AndroidVersion.isAtLeastAndroid14) dynamic(context, kind) else dark(context)
      }

    return palette.withTransparency(storedTransparency)
  }

  private fun WidgetPalette.withTransparency(transparencyPercent: Int): WidgetPalette {
    if (transparencyPercent <= 0) return this
    val percent = transparencyPercent.coerceIn(0, 100)
    val alpha = ((100 - percent) * 255 / 100).coerceIn(0, 255)
    val statusAlpha = (Color.alpha(statusBackground) * (100 - percent) / 100).coerceIn(0, 255)
    return copy(
      background = ColorUtils.setAlphaComponent(background, alpha),
      searchBackground = ColorUtils.setAlphaComponent(searchBackground, alpha),
      statusBackground = ColorUtils.setAlphaComponent(statusBackground, statusAlpha),
    )
  }

  @RequiresApi(UPSIDE_DOWN_CAKE)
  private fun dynamic(
    context: Context,
    kind: WidgetPaletteKind,
  ) = when (kind) {
    WidgetPaletteKind.DYNAMIC_LIGHT -> dynamicLight(context)
    WidgetPaletteKind.DYNAMIC_BLACK -> dynamicBlack(context)
    else -> dynamicDark(context)
  }

  /**
   * Whether the launcher is drawing dark right now.
   *
   * Read from the app's own configuration rather than the launcher's, which is not ours to ask for.
   * The two agree in practice - the night mode is a device setting - and where they cannot, the widget corrects itself on its next update.
   */
  private fun Context.isSystemDark() = resources.configuration.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES

  private fun light(context: Context) =
    WidgetPalette(
      background = context.color(R.color.widgetLightBackground),
      textPrimary = context.color(R.color.widgetLightTextPrimary),
      textSecondary = context.color(R.color.widgetLightTextSecondary),
      accent = context.color(R.color.widgetLightAccent),
      accentText = context.color(R.color.widgetLightAccentText),
      badge = context.color(R.color.widgetLightBadge),
      placeholderInk = context.color(R.color.widgetLightPlaceholderInk),
      searchBackground = context.color(R.color.widgetLightSearchBackground),
      statusBackground = context.color(R.color.widgetLightStatusBackground),
      statusText = context.color(R.color.widgetLightStatusText),
      mediaFrame = R.drawable.bg_widget_media_light,
    )

  private fun dark(context: Context) =
    WidgetPalette(
      background = context.color(R.color.widgetDarkBackground),
      textPrimary = context.color(R.color.widgetDarkTextPrimary),
      textSecondary = context.color(R.color.widgetDarkTextSecondary),
      accent = context.color(R.color.widgetDarkAccent),
      accentText = context.color(R.color.widgetDarkAccentText),
      badge = context.color(R.color.widgetDarkBadge),
      placeholderInk = context.color(R.color.widgetDarkPlaceholderInk),
      searchBackground = context.color(R.color.widgetDarkSearchBackground),
      statusBackground = context.color(R.color.widgetDarkStatusBackground),
      statusText = context.color(R.color.widgetDarkStatusText),
      mediaFrame = R.drawable.bg_widget_media_dark,
    )

  private fun black(context: Context) =
    WidgetPalette(
      background = context.color(R.color.widgetBlackBackground),
      textPrimary = context.color(R.color.widgetBlackTextPrimary),
      textSecondary = context.color(R.color.widgetBlackTextSecondary),
      accent = context.color(R.color.widgetBlackAccent),
      accentText = context.color(R.color.widgetBlackAccentText),
      badge = context.color(R.color.widgetBlackBadge),
      placeholderInk = context.color(R.color.widgetBlackPlaceholderInk),
      searchBackground = context.color(R.color.widgetBlackSearchBackground),
      statusBackground = context.color(R.color.widgetBlackStatusBackground),
      statusText = context.color(R.color.widgetBlackStatusText),
      mediaFrame = R.drawable.bg_widget_media_black,
    )

  /**
   * The three below take their colours from the framework's wallpaper roles, so they carry no entries in colors_widget.xml.
   * The roles chosen are the ones @style/ThemeOverlay.Shelfly.MaterialYou maps the same app attributes to in values-v34 and values-night-v34; keep the two in step.
   *
   * The scrim behind the label is the exception: there is no translucent role to point at, so it stays the literal the static palettes use.
   */
  @RequiresApi(UPSIDE_DOWN_CAKE)
  private fun dynamicLight(context: Context) =
    WidgetPalette(
      background = context.color(android.R.color.system_neutral1_10),
      textPrimary = context.color(android.R.color.system_neutral1_900),
      textSecondary = context.color(android.R.color.system_neutral2_700),
      accent = context.color(android.R.color.system_accent1_600),
      accentText = context.color(android.R.color.system_accent1_700),
      badge = context.color(android.R.color.system_neutral2_100),
      placeholderInk = context.color(android.R.color.system_neutral2_500),
      searchBackground = context.color(android.R.color.system_neutral1_50),
      statusBackground = context.color(R.color.widgetLightStatusBackground),
      statusText = context.color(android.R.color.system_neutral2_700),
      mediaFrame = R.drawable.bg_widget_media_dynamic_light,
    )

  @RequiresApi(UPSIDE_DOWN_CAKE)
  private fun dynamicDark(context: Context) =
    WidgetPalette(
      background = context.color(android.R.color.system_surface_dark),
      textPrimary = context.color(android.R.color.system_neutral1_50),
      textSecondary = context.color(android.R.color.system_neutral2_200),
      accent = context.color(android.R.color.system_accent1_600),
      accentText = context.color(android.R.color.system_accent1_200),
      badge = context.color(android.R.color.system_surface_container_high_dark),
      placeholderInk = context.color(android.R.color.system_neutral2_400),
      searchBackground = context.color(android.R.color.system_surface_container_dark),
      statusBackground = context.color(R.color.widgetDarkStatusBackground),
      statusText = context.color(android.R.color.system_neutral1_50),
      mediaFrame = R.drawable.bg_widget_media_dynamic_dark,
    )

  /**
   * Material You with the pure black switch on: the surfaces go black, the wallpaper stays on everything drawn over them.
   * Which surfaces move is decided by @style/ThemeOverlay.Shelfly.Amoled, since that overlay is applied last in the app and wins over the dynamic ones.
   */
  @RequiresApi(UPSIDE_DOWN_CAKE)
  private fun dynamicBlack(context: Context) =
    dynamicDark(context).copy(
      background = context.color(R.color.widgetBlackBackground),
      badge = context.color(R.color.widgetBlackBadge),
      searchBackground = context.color(R.color.widgetBlackSearchBackground),
      mediaFrame = R.drawable.bg_widget_media_dynamic_black,
    )

  private fun Context.color(colorResId: Int) = ContextCompat.getColor(this, colorResId)
}
