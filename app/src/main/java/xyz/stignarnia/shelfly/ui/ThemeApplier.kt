package xyz.stignarnia.shelfly.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
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

  /**
   * Applies the night mode for the stored theme.
   * Belongs in Application.onCreate: AppCompat resolves it once per process, and doing it later costs a recreate.
   */
  fun applyNightMode(settingsRepository: SettingsRepository) {
    val theme = AppTheme.fromId(settingsRepository.themeId)
    AppCompatDelegate.setDefaultNightMode(theme.nightMode)
  }

  /**
   * Applies the overlays.
   * Call before super.onCreate, so the theme is settled before anything inflates against it.
   *
   * The AMOLED overlay is applied whenever the switch is on, without asking whether the app is currently dark.
   * It cannot ask - under "Follow system" that answer belongs to the configuration, not to us - so instead the overlay's own colours are qualified, carrying the ordinary light surfaces in values/ and black in values-night/.
   * By day it therefore resolves to a no-op.
   */
  fun applyOverlays(activity: Activity) {
    val settingsRepository = EntryPointAccessors
      .fromApplication(activity.applicationContext, ThemeEntryPoint::class.java)
      .settingsRepository()

    if (AppTheme.fromId(settingsRepository.themeId).isDynamic) {
      // Not every API 31 device ships wallpaper colours.
      // This no-ops where they are missing, leaving the static palette rather than a half applied one.
      DynamicColors.applyToActivityIfAvailable(
        activity,
        DynamicColorsOptions
          .Builder()
          .setThemeOverlay(R.style.ThemeOverlay_Shelfly_MaterialYou)
          .build(),
      )
    }

    if (settingsRepository.isAmoled) {
      activity.theme.applyStyle(R.style.ThemeOverlay_Shelfly_Amoled, true)
    }
  }
}
