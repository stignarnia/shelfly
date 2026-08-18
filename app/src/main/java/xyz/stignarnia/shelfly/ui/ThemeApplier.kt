package xyz.stignarnia.shelfly.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_settings.helpers.AppTheme

/**
 * Puts the selected theme onto an Activity.
 *
 * The app has one theme style, @style/AppTheme, with a DayNight parent.
 * Light and dark are that style resolved against values/ and values-night/, chosen by the night mode; AMOLED and Material You are overlays composed on top of it.
 * That is how six entries and one switch produce every combination without needing a style per combination.
 *
 * Order matters: Material You first, then AMOLED, so a pure black background wins over the dynamic surface and "Material You Dark" with AMOLED on is black carrying wallpaper accents rather than a dynamic dark grey.
 */
object ThemeApplier {

  /**
   * Reaches the settings before Hilt has injected the Activity.
   *
   * Activity field injection is dispatched from inside ComponentActivity.onCreate, so an @Inject field is still null in the window before super.onCreate - which is exactly where a theme has to be applied.
   * The singleton component is already built by then, so going through it directly is the way in.
   */
  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface ThemeEntryPoint {
    fun settingsRepository(): SettingsRepository
  }

  private fun settings(activity: Activity) =
    EntryPointAccessors
      .fromApplication(activity.applicationContext, ThemeEntryPoint::class.java)
      .settingsRepository()

  /**
   * Applies the night mode for the stored theme.
   *
   * Call before super.onCreate, and from Application.onCreate for the first launch of a process.
   * By this point the settings screen has already set it, so it is normally a no-op; the guard keeps it that way rather than letting a redundant set trigger AppCompat's own recreate.
   */
  fun applyNightMode(settingsRepository: SettingsRepository) {
    val theme = AppTheme.fromId(settingsRepository.themeId)
    if (AppCompatDelegate.getDefaultNightMode() != theme.nightMode) {
      AppCompatDelegate.setDefaultNightMode(theme.nightMode)
    }
  }

  fun applyNightMode(activity: Activity) = applyNightMode(settings(activity))

  /**
   * Applies the overlays.
   * Call before super.onCreate, so the theme is settled before anything inflates against it.
   *
   * Both go on with applyStyle rather than through DynamicColors.applyToActivityIfAvailable.
   * That helper gates on a manufacturer and brand allowlist and skips in silence when it does not recognise the device; the overlay names @android:color/system_* directly, and those are framework resources on every API 34 device, so there is nothing to detect and nothing to be excluded from.
   *
   * The AMOLED overlay is applied whenever the switch is on, without asking whether the app is currently dark.
   * It cannot ask - under "Follow system" that answer belongs to the configuration, not to us - so instead the overlay's own colours are qualified, carrying the ordinary light surfaces in values/ and black in values-night/.
   * By day it therefore resolves to a no-op.
   */
  fun applyOverlays(activity: Activity) {
    val settingsRepository = settings(activity)

    if (AppTheme.fromId(settingsRepository.themeId).isDynamic) {
      activity.theme.applyStyle(R.style.ThemeOverlay_Shelfly_MaterialYou, true)
    }

    if (settingsRepository.isAmoled) {
      activity.theme.applyStyle(R.style.ThemeOverlay_Shelfly_Amoled, true)
    }
  }
}
