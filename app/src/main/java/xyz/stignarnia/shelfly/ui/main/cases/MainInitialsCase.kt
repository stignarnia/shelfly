package xyz.stignarnia.shelfly.ui.main.cases

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.shelfly.BuildConfig
import xyz.stignarnia.ui_base.common.AppCountry
import xyz.stignarnia.ui_base.utilities.extensions.withApiAtLeast
import xyz.stignarnia.ui_settings.helpers.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@ViewModelScoped
class MainInitialsCase @Inject constructor(
  @ApplicationContext private val context: Context,
  private val settingsRepository: SettingsRepository,
  @Named("miscPreferences") private var miscPreferences: SharedPreferences,
) {

  companion object {
    private const val KEY_APP_VERSION = "APP_VERSION"
    private const val KEY_APP_VERSION_NAME = "APP_VERSION_NAME"
  }

  suspend fun setInitialRun(value: Boolean) {
    val settings = settingsRepository.load()
    settings.let {
      settingsRepository.update(it.copy(isInitialRun = value))
    }
  }

  suspend fun isInitialRun(): Boolean {
    val settings = settingsRepository.load()
    return settings.isInitialRun
  }

  fun setInitialCountry() {
    var country = (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.simCountryIso
    if (country == null) {
      val locale = LocaleListCompat.getAdjustedDefault()
      country = if (locale.size() > 1) {
        locale.get(1)?.country
      } else {
        locale.get(0)?.country
      }
    }
    if (!country.isNullOrBlank()) {
      AppCountry.values().forEach { appCountry ->
        if (appCountry.code.equals(country, ignoreCase = true)) {
          settingsRepository.country = appCountry.code
          return
        }
      }
    }
  }

  suspend fun setInitialNotifications() {
    withApiAtLeast(33) {
      val settings = settingsRepository.load()
      settings.let {
        settingsRepository.update(it.copy(episodesNotificationsEnabled = false))
      }
    }
  }

  fun setLanguage(appLanguage: AppLanguage) {
    settingsRepository.language = appLanguage.code
    val locales = LocaleListCompat.forLanguageTags(appLanguage.code)
    AppCompatDelegate.setApplicationLocales(locales)
  }

  /**
   * The language the device itself is set to, which is not the same thing as the
   * app's own locale: once a per app locale has been applied,
   * [LocaleListCompat.getAdjustedDefault] reports that one first. The welcome
   * flow re-offers the device language whenever it changes, so it needs the
   * system value on every launch rather than only before the first choice.
   *
   * Null when the device is set to a language the app has no translation for.
   * That is not the same as English, and answering it with English would offer
   * a user running the app in Italian on a Japanese phone a switch to English
   * they never asked about.
   */
  fun detectSystemLanguage(): AppLanguage? {
    val locales = LocaleManagerCompat.getSystemLocales(context)
    for (index in 0 until locales.size()) {
      val language = locales[index]?.language?.lowercase() ?: continue
      AppLanguage.entries
        .firstOrNull { it.code == language }
        ?.let { return it }
    }
    return null
  }

  /**
   * When the notes are due to be shown the version stamp is deliberately left
   * alone: [setWhatsNewSeen] writes it once the user has actually closed them,
   * so an upgrade whose notes were never read is offered again. When nothing
   * will be shown for this build the stamp is written right away, otherwise the
   * next launch would mistake the build for an unread upgrade.
   */
  fun showWhatsNew(isInitialRun: Boolean): Boolean {
    val version = miscPreferences.getInt(KEY_APP_VERSION, 0)
    val name = miscPreferences.getString(KEY_APP_VERSION_NAME, "")

    fun isPatchUpdate(): Boolean {
      if (name.isNullOrBlank()) return false

      val major = name.split(".").getOrNull(0)?.toIntOrNull()
      val minor = name.split(".").getOrNull(1)?.toIntOrNull()

      val currentMajor = BuildConfig.VERSION_NAME
        .split(".")
        .getOrNull(0)
        ?.toIntOrNull()
      val currentMinor = BuildConfig.VERSION_NAME
        .split(".")
        .getOrNull(1)
        ?.toIntOrNull()

      return (major == currentMajor) && (minor == currentMinor)
    }

    val showWhatsNew = Config.SHOW_WHATS_NEW &&
      BuildConfig.VERSION_CODE > version &&
      BuildConfig.VERSION_NAME != name &&
      !isInitialRun &&
      !isPatchUpdate()

    if (!showWhatsNew) {
      setWhatsNewSeen()
    }

    return showWhatsNew
  }

  fun setWhatsNewSeen() {
    miscPreferences.edit {
      putInt(KEY_APP_VERSION, BuildConfig.VERSION_CODE)
      putString(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
    }
  }

  fun saveInstallTimestamp() {
    if (settingsRepository.installTimestamp == 0L) {
      settingsRepository.installTimestamp = nowUtcMillis()
      Timber.d("Installation timestamp saved: ${nowUtc()}")
    }
  }
}
