package xyz.stignarnia.shelfly

import android.app.Application
import android.app.NotificationChannel
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.shelfly.ui.ThemeApplier
import xyz.stignarnia.ui_base.common.AppScopeProvider
import xyz.stignarnia.ui_base.common.WidgetsProvider
import xyz.stignarnia.ui_base.notifications.NotificationChannel as AppNotificationChannel
import xyz.stignarnia.ui_base.notifications.SyncNotificationManager
import xyz.stignarnia.ui_base.utilities.AndroidVersion
import xyz.stignarnia.ui_base.utilities.extensions.notificationManager
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_widgets.calendar.CalendarWidgetProvider
import xyz.stignarnia.ui_widgets.calendar_movies.CalendarMoviesWidgetProvider
import xyz.stignarnia.ui_widgets.progress.ProgressWidgetProvider
import xyz.stignarnia.ui_widgets.progress_movies.ProgressMoviesWidgetProvider

@HiltAndroidApp
class App :
  Application(),
  AppScopeProvider,
  Configuration.Provider,
  WidgetsProvider {

  override val appScope = MainScope()

  @Inject lateinit var workerFactory: HiltWorkerFactory
  @Inject lateinit var settingsRepository: SettingsRepository
  @Inject lateinit var syncNotificationManager: SyncNotificationManager

  override val workManagerConfiguration: Configuration
    get() = Configuration
      .Builder()
      .setWorkerFactory(workerFactory)
      .build()

  override fun onCreate() {
    fun setupSettings() =
      runBlocking {
        if (!settingsRepository.isInitialized()) {
          settingsRepository.update(Settings.createInitial())
        }
      }

    /**
     * Pin the stored language, which is the app's own source of truth for it.
     * Applied unconditionally: re-applying the language already in force is a no-op, whereas reading back the current one this early is not reliable.
     *
     * Note this only takes effect from the following launch on API 33+, where it goes through the system LocaleManager asynchronously.
     * The welcome flow therefore does not depend on it - see WelcomeState.displayLanguage.
     */
    fun setupLanguage() {
      settingsRepository.isLocaleInitialised = true
      AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(settingsRepository.language),
      )
    }

    fun setupStrictMode() {
      if (BuildConfig.DEBUG) {
        StrictMode
          .setThreadPolicy(
            StrictMode.ThreadPolicy
              .Builder()
              .detectAll()
              .penaltyLog()
              .build(),
          )
        if (AndroidVersion.isAtLeastAndroid12) {
          StrictMode.setVmPolicy(
            StrictMode.VmPolicy
              .Builder()
              .detectUnsafeIntentLaunch()
              .penaltyDeath()
              .build(),
          )
        }
      }
    }

    fun setupNotificationChannels() {
      if (!AndroidVersion.isAtLeastAndroid8) return

      fun createChannel(channel: AppNotificationChannel) =
        NotificationChannel(
          // id =
          channel.name,
          // name =
          channel.displayName,
          // importance =
          channel.importance,
        ).apply {
          description = channel.description
        }

      notificationManager().run {
        createNotificationChannel(createChannel(AppNotificationChannel.GENERAL_INFO))
        createNotificationChannel(createChannel(AppNotificationChannel.SHOWS_INFO))
        createNotificationChannel(createChannel(AppNotificationChannel.EPISODES_ANNOUNCEMENTS))
        createNotificationChannel(createChannel(AppNotificationChannel.MOVIES_ANNOUNCEMENTS))
        createNotificationChannel(createChannel(AppNotificationChannel.SYNC))
      }
    }

    super.onCreate()

    if (ProcessPhoenix.isPhoenixProcess(this)) return

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    setupSettings()
    setupLanguage()
    ThemeApplier.applyNightMode(settingsRepository)
    setupStrictMode()
    setupNotificationChannels()
    syncNotificationManager.cancelStaleProgress()
  }

  override fun requestShowsWidgetsUpdate() {
    appScope.launch {
      ProgressWidgetProvider.requestUpdate(applicationContext)
      CalendarWidgetProvider.requestUpdate(applicationContext)
    }
  }

  override fun requestMoviesWidgetsUpdate() {
    appScope.launch {
      ProgressMoviesWidgetProvider.requestUpdate(applicationContext)
      CalendarMoviesWidgetProvider.requestUpdate(applicationContext)
    }
  }
}
