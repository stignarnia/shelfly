package xyz.stignarnia.uiSettings.helpers

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import xyz.stignarnia.uiBase.utilities.AndroidVersion
import xyz.stignarnia.uiSettings.R

/**
 * What the Theme row offers.
 *
 * A theme here is not a style of its own.
 * It is a night mode, optionally with the Material You overlay on top, and AMOLED is a third axis stored separately - see [xyz.stignarnia.repository.settings.SettingsRepository.isAmoled] - because it applies to whichever of these can end up dark.
 * Six entries and one switch cover every combination; folding AMOLED in here would need ten.
 *
 * [id] is what gets persisted, so these names are storage and must not be renamed casually.
 */
enum class AppTheme(
  val id: String,
  @get:StringRes val displayName: Int,
  val nightMode: Int,
  val isDynamic: Boolean = false,
) {
  SYSTEM("SYSTEM", R.string.textThemeSystem, MODE_NIGHT_FOLLOW_SYSTEM),
  LIGHT("LIGHT", R.string.textThemeLight, MODE_NIGHT_NO),
  DARK("DARK", R.string.textThemeDark, MODE_NIGHT_YES),

  DYNAMIC_SYSTEM(
    id = "DYNAMIC_SYSTEM",
    displayName = R.string.textThemeMaterialYou,
    nightMode = MODE_NIGHT_FOLLOW_SYSTEM,
    isDynamic = true,
  ),
  DYNAMIC_LIGHT(
    id = "DYNAMIC_LIGHT",
    displayName = R.string.textThemeMaterialYouLight,
    nightMode = MODE_NIGHT_NO,
    isDynamic = true,
  ),
  DYNAMIC_DARK(
    id = "DYNAMIC_DARK",
    displayName = R.string.textThemeMaterialYouDark,
    nightMode = MODE_NIGHT_YES,
    isDynamic = true,
  ),
  ;

  /**
   * Whether this theme can ever resolve to a dark one, and so whether the AMOLED switch has anything to act on.
   * Expressed as a property rather than a list of names, so a theme added later cannot be forgotten.
   */
  val canBeDark: Boolean
    get() = nightMode != MODE_NIGHT_NO

  /**
   * Whether this device can honour the theme.
   * Only the dynamic ones are version-dependent, and they need the API 34 surface roles - see values-night-v34.
   */
  val isSupported: Boolean
    get() = !isDynamic || AndroidVersion.isAtLeastAndroid14

  companion object {
    /**
     * The entries this device can actually show.
     * Material You needs the API 34 surface roles: the tonal ramp available from 31 has no dark ground between a washed out #0A1A3D and flat black.
     */
    fun supported() = entries.filter { it.isSupported }

    /**
     * Resolves a stored id, falling back to the nearest theme this device can honour.
     * A backup restored from a Pixel onto an older phone names a Material You theme that cannot be applied here, and dropping to the same brightness without the wallpaper is closer to the user's intent than dropping to the default.
     */
    fun fromId(id: String?): AppTheme {
      val stored = entries.firstOrNull { it.id == id } ?: DARK
      if (stored.isSupported) return stored
      return entries.first { !it.isDynamic && it.nightMode == stored.nightMode }
    }
  }
}
