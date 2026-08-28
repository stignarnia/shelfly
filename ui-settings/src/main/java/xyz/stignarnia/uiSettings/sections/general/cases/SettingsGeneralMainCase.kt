package xyz.stignarnia.uiSettings.sections.general.cases

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.common.AppCountry
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.dates.AppDateFormat
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiBase.utilities.AndroidVersion
import xyz.stignarnia.uiModel.ProgressDateSelectionType
import xyz.stignarnia.uiModel.ProgressNextEpisodeType
import xyz.stignarnia.uiModel.Settings
import xyz.stignarnia.uiSettings.helpers.AppLanguage
import xyz.stignarnia.uiSettings.helpers.AppTheme
import javax.inject.Inject

@ViewModelScoped
class SettingsGeneralMainCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val settingsRepository: SettingsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun getSettings(): Settings =
      withContext(dispatchers.IO) {
        settingsRepository.load()
      }

    suspend fun setRecentShowsAmount(amount: Int) {
      check(amount in Config.MY_SHOWS_RECENTS_OPTIONS)
      withContext(dispatchers.IO) {
        val settings = settingsRepository.load()
        settings.let {
          val new = it.copy(myRecentsAmount = amount)
          settingsRepository.update(new)
        }
      }
    }

    suspend fun enableSpecialSeasons(enable: Boolean) {
      val settings = settingsRepository.load()
      settings.let {
        val new = it.copy(specialSeasonsEnabled = enable)
        settingsRepository.update(new)
      }
    }

    fun isMoviesEnabled() = settingsRepository.isMoviesEnabled

    suspend fun enableMovies(enable: Boolean) {
      val newMode = if (!enable) Mode.SHOWS else settingsRepository.mode
      settingsRepository.run {
        isMoviesEnabled = enable
        mode = newMode
      }
      announcementManager.refreshMoviesAnnouncements()
    }

    fun isStreamingsEnabled() = settingsRepository.streamingsEnabled

    fun enableStreamings(enable: Boolean) {
      settingsRepository.run {
        streamingsEnabled = enable
      }
    }

    suspend fun getLanguage(): AppLanguage {
      if (AndroidVersion.isAtLeastAndroid13) {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
          val locale = locales.get(0)!!.language
          val language = AppLanguage.fromCode(locale)
          if (settingsRepository.language != locale) {
            setLanguage(language)
          }
          return language
        }
      }
      return AppLanguage.fromCode(settingsRepository.language)
    }

    suspend fun setLanguage(language: AppLanguage) {
      settingsRepository.run {
        this.language = language.code
        val unused =
          AppLanguage.entries
            .filter { it.code != Config.DEFAULT_LANGUAGE && it != language }
            .map { it.code }
        clearUnusedTranslations(unused)
        clearLanguageLogs()
      }
    }

    fun getTheme() = AppTheme.fromId(settingsRepository.themeId)

    fun isAmoled() = settingsRepository.isAmoled

    /**
     * The widgets follow the app's theme unless they were configured otherwise, so a change here has to reach them.
     * They are drawn with colours pushed into the launcher rather than resolved from a theme, so nothing repaints them on its own.
     */
    fun setAmoled(
      enabled: Boolean,
      context: Context,
    ) {
      settingsRepository.isAmoled = enabled
      (context.applicationContext as WidgetsProvider).requestAllWidgetsUpdate()
    }

    fun setTheme(
      theme: AppTheme,
      context: Context,
    ) {
      settingsRepository.themeId = theme.id
      (context.applicationContext as WidgetsProvider).requestAllWidgetsUpdate()
    }

    fun getCountry() = AppCountry.fromCode(settingsRepository.country)

    fun setCountry(country: AppCountry) {
      settingsRepository.country = country.code
    }

    fun getProgressType() = settingsRepository.progressNextEpisodeType

    fun setProgressType(type: ProgressNextEpisodeType) {
      settingsRepository.progressNextEpisodeType = type
    }

    fun getDateSelectionType() = settingsRepository.progressDateSelectionType

    fun setDateSelectionType(type: ProgressDateSelectionType) {
      settingsRepository.progressDateSelectionType = type
    }

    fun getProgressUpcomingDays() = settingsRepository.progressUpcomingDays

    fun setProgressUpcomingDays(days: Long) {
      settingsRepository.progressUpcomingDays = days
      if (days == 0L) {
        settingsRepository.filters.progressShowsUpcoming = false
      }
    }

    fun setDateFormat(
      format: AppDateFormat,
      context: Context,
    ) {
      settingsRepository.dateFormat = format.name
      (context.applicationContext as WidgetsProvider).run {
        requestShowsWidgetsUpdate()
        requestMoviesWidgetsUpdate()
      }
    }

    fun getDateFormat() = AppDateFormat.valueOf(settingsRepository.dateFormat)

    fun setTabletsColumns(columns: Int) {
      settingsRepository.viewMode.tabletGridSpanSize = columns
    }

    fun getTabletsColumns(): Int = settingsRepository.viewMode.tabletGridSpanSize
  }
