package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.stignarnia.common.Config.DEFAULT_COUNTRY
import xyz.stignarnia.common.Config.DEFAULT_DATE_FORMAT
import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.utilities.BooleanPreference
import xyz.stignarnia.repository.utilities.EnumPreference
import xyz.stignarnia.repository.utilities.LongPreference
import xyz.stignarnia.repository.utilities.StringPreference
import xyz.stignarnia.ui_model.ProgressDateSelectionType
import xyz.stignarnia.ui_model.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.ui_model.ProgressNextEpisodeType
import xyz.stignarnia.ui_model.ProgressNextEpisodeType.LAST_WATCHED
import xyz.stignarnia.ui_model.ProgressType
import xyz.stignarnia.ui_model.Settings
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
  val sorting: SettingsSortRepository,
  val filters: SettingsFiltersRepository,
  val widgets: SettingsWidgetsRepository,
  val viewMode: SettingsViewModeRepository,
  val spoilers: SettingsSpoilersRepository,
  val sync: SettingsSyncRepository,
  val webdav: SettingsWebDavRepository,
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
  @Named("miscPreferences") private var preferences: SharedPreferences,
) {

  companion object Key {
    const val LANGUAGE = "KEY_LANGUAGE"
    private const val THEME_ID = "KEY_THEME_ID"
    private const val THEME_AMOLED = "KEY_THEME_AMOLED"
    private const val COUNTRY = "KEY_COUNTRY"
    private const val DATE_FORMAT = "KEY_DATE_FORMAT"
    private const val MODE = "KEY_MOVIES_MODE"
    private const val MOVIES_ENABLED = "KEY_MOVIES_ENABLED"
    private const val PROGRESS_PERCENT = "KEY_PROGRESS_PERCENT"
    private const val STREAMINGS_ENABLED = "KEY_STREAMINGS_ENABLED"
    private const val INSTALL_TIMESTAMP = "INSTALL_TIMESTAMP"
    private const val PROGRESS_UPCOMING_COLLAPSED = "PROGRESS_UPCOMING_COLLAPSED"
    private const val PROGRESS_UPCOMING_DAYS = "PROGRESS_UPCOMING_DAYS"
    private const val PROGRESS_ON_HOLD_COLLAPSED = "PROGRESS_ON_HOLD_COLLAPSED"
    private const val PROGRESS_NEXT_EPISODE_TYPE = "PROGRESS_NEXT_EPISODE_TYPE"
    private const val PROGRESS_DATE_SELECTION_TYPE = "PROGRESS_DATE_SELECTION_TYPE"
    private const val LOCALE_INITIALISED = "LOCALE_INITIALISED"
  }

  suspend fun isInitialized() =
    withContext(dispatchers.IO) {
      localSource.settings.getCount() > 0
    }

  suspend fun load(): Settings {
    val settingsDb = withContext(dispatchers.IO) {
      localSource.settings.getAll()
    }
    return mappers.settings.fromDatabase(settingsDb)
  }

  suspend fun update(settings: Settings) {
    withContext(dispatchers.IO) {
      transactions.withTransaction {
        val settingsDb = mappers.settings.toDatabase(settings)
        localSource.settings.upsert(settingsDb)
      }
    }
  }

  var installTimestamp by LongPreference(preferences, INSTALL_TIMESTAMP, 0L)
  var streamingsEnabled by BooleanPreference(preferences, STREAMINGS_ENABLED, true)
  var isMoviesEnabled by BooleanPreference(preferences, MOVIES_ENABLED, true)

  /**
   * The selected theme's id, as named by AppTheme in ui-settings.
   * Stored as a string for the same reason language and country are: the enum lives in the UI layer and this module cannot see it.
   *
   * The default is the theme the app shipped with before it had any others.
   * No migration is needed from the old int key - the picker only ever offered one entry, so every existing install was already on dark.
   */
  var themeId by StringPreference(preferences, THEME_ID, "DARK")

  /**
   * Whether to black out backgrounds wherever the theme resolves to dark.
   * Independent of [themeId] on purpose: the switch keeps its value while a light theme is selected, so coming back to a dark one restores what the user chose.
   */
  var isAmoled by BooleanPreference(preferences, THEME_AMOLED)
  var language by StringPreference(preferences, LANGUAGE, DEFAULT_LANGUAGE)
  var country by StringPreference(preferences, COUNTRY, DEFAULT_COUNTRY)
  var dateFormat by StringPreference(preferences, DATE_FORMAT, DEFAULT_DATE_FORMAT)

  var progressUpcomingDays by LongPreference(preferences, PROGRESS_UPCOMING_DAYS, 30)
  var isProgressUpcomingCollapsed by BooleanPreference(preferences, PROGRESS_UPCOMING_COLLAPSED)
  var isProgressOnHoldCollapsed by BooleanPreference(preferences, PROGRESS_ON_HOLD_COLLAPSED)
  var progressNextEpisodeType by EnumPreference(
    preferences,
    PROGRESS_NEXT_EPISODE_TYPE,
    LAST_WATCHED,
    ProgressNextEpisodeType::class.java,
  )
  var progressDateSelectionType by EnumPreference(
    preferences,
    PROGRESS_DATE_SELECTION_TYPE,
    ALWAYS_ASK,
    ProgressDateSelectionType::class.java,
  )
  var isLocaleInitialised by BooleanPreference(preferences, LOCALE_INITIALISED, false)

  var mode: Mode
    get() {
      val default = Mode.SHOWS.name
      return Mode.valueOf(preferences.getString(MODE, default) ?: default)
    }
    set(value) = preferences.edit(true) { putString(MODE, value.name) }

  var progressPercentType: ProgressType
    get() {
      val setting = preferences.getString(PROGRESS_PERCENT, ProgressType.AIRED.name) ?: ProgressType.AIRED.name
      return ProgressType.valueOf(setting)
    }
    set(value) = preferences.edit(true) { putString(PROGRESS_PERCENT, value.name) }

  suspend fun clearLanguageLogs() {
    with(localSource) {
      transactions.withTransaction {
        translationsShowsSyncLog.deleteAll()
        translationsMoviesSyncLog.deleteAll()
      }
    }
  }

  suspend fun clearUnusedTranslations(input: List<String>) {
    with(localSource) {
      transactions.withTransaction {
        showTranslations.deleteByLanguage(input)
        movieTranslations.deleteByLanguage(input)
        episodesTranslations.deleteByLanguage(input)
        people.deleteTranslations()
        movieImages.deleteAll()
        showImages.deleteAll()
      }
    }
  }
}
