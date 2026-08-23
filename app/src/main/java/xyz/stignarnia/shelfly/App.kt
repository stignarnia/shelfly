package xyz.stignarnia.shelfly

import android.app.Application
import android.app.NotificationChannel
import android.content.ComponentName
import android.content.pm.PackageManager
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
import xyz.stignarnia.ui_widgets.search.SearchWidgetProvider

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

  /** The night mode the widgets were last drawn against - see [onConfigurationChanged]. */
  private var lastNightMode = android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED

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
              // Logged as well as fatal.
              // Death on its own leaves a SIGKILL with nothing to read - no trace, no crash record - which is indistinguishable from the app simply vanishing.
              .penaltyLog()
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
    setupWidgets()
    ThemeApplier.applyNightMode(settingsRepository)
    lastNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    setupStrictMode()
    setupNotificationChannels()
    syncNotificationManager.cancelStaleProgress()
  }

  /**
   * Takes the widgets off devices that cannot draw them properly.
   *
   * A widget's rows travel inside its views - see WidgetCollection - which needs the collection API that arrives in Android 12, and the same version is where RemoteViews learned to tint a background, which is what themes them.
   * The alternative, a RemoteViewsService serving rows one at a time, is the one the framework has stopped delivering updates for: it leaves a widget that cannot repaint and cannot be themed.
   *
   * Disabling the receivers is what removes them from the launcher's picker; a widget already on a home screen from an older build stops being offered and is dropped by the launcher.
   */
  private fun setupWidgets() {
    if (AndroidVersion.isAtLeastAndroid12) return

    val providers = listOf(
      ProgressWidgetProvider::class.java,
      ProgressMoviesWidgetProvider::class.java,
      CalendarWidgetProvider::class.java,
      CalendarMoviesWidgetProvider::class.java,
      SearchWidgetProvider::class.java,
    )
    providers.forEach {
      packageManager.setComponentEnabledSetting(
        ComponentName(this, it),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
      )
    }
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

  override fun requestAllWidgetsUpdate() {
    requestShowsWidgetsUpdate()
    requestMoviesWidgetsUpdate()
    appScope.launch { SearchWidgetProvider.requestUpdate(applicationContext) }
  }

  /**
   * Repaints the widgets when the system flips between light and dark.
   *
   * A widget under "Follow system" has its colours resolved when it is drawn and pushed into the launcher as values, so nothing about a configuration change corrects them by itself.
   * This catches the case where the app happens to be running; where it is not, the widget's own update period does, within fifteen minutes.
   * A manifest receiver is not an option - ACTION_CONFIGURATION_CHANGED has not been deliverable to one since Android 8.
   */
  override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
    super.onConfigurationChanged(newConfig)
    val nightMode = newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    if (nightMode == lastNightMode) return
    lastNightMode = nightMode
    requestAllWidgetsUpdate()
  }
}
