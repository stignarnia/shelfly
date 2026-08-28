package xyz.stignarnia.uiSettings.sections.general

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.AppCountry
import xyz.stignarnia.uiBase.dates.AppDateFormat
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.ProgressDateSelectionType
import xyz.stignarnia.uiModel.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.uiModel.ProgressDateSelectionType.NOW
import xyz.stignarnia.uiModel.ProgressNextEpisodeType
import xyz.stignarnia.uiModel.ProgressNextEpisodeType.LAST_WATCHED
import xyz.stignarnia.uiModel.ProgressNextEpisodeType.OLDEST
import xyz.stignarnia.uiModel.Settings
import xyz.stignarnia.uiSettings.R
import xyz.stignarnia.uiSettings.databinding.FragmentSettingsGeneralBinding
import xyz.stignarnia.uiSettings.helpers.AppLanguage
import xyz.stignarnia.uiSettings.helpers.AppTheme
import java.util.Locale

@AndroidEntryPoint
class SettingsGeneralFragment : BaseFragment<SettingsGeneralViewModel>(R.layout.fragment_settings_general) {
  companion object {
    /**
     * Matches the disabled alpha the button and chip state lists already use, so a greyed row reads the same as every other disabled control.
     */
    private const val DISABLED_ALPHA = 0.5F
  }

  override val viewModel by viewModels<SettingsGeneralViewModel>()
  private val binding by viewBinding(FragmentSettingsGeneralBinding::bind)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      doAfterLaunch = { viewModel.loadSettings() },
    )
  }

  private fun setupView() {
    with(binding) {
      settingsTabletColumns.visibleIf(isTablet)

      settingsIncludeSpecials.onClick {
        viewModel.enableSpecialSeasons(!settingsIncludeSpecialsSwitch.isChecked)
      }
      settingsMoviesEnabled.onClick {
        viewModel.enableMovies(!settingsMoviesEnabledSwitch.isChecked)
      }
      settingsStreamingsEnabled.onClick {
        viewModel.enableStreamings(!settingsStreamingsEnabledSwitch.isChecked)
      }
    }
  }

  private fun render(uiState: SettingsGeneralUiState) {
    with(binding) {
      with(uiState) {
        settingsRecentShowsAmount.onClick { showRecentShowsDialog(settings) }

        settingsMoviesEnabledSwitch.isChecked = moviesEnabled
        settingsStreamingsEnabledSwitch.isChecked = streamingsEnabled

        renderSettings(settings)
        renderLanguage(language)
        renderTheme(theme, amoled)
        renderCountry(country)
        renderProgressType(progressNextType)
        renderDateSelection(progressDateSelectionType)
        renderProgressUpcoming(progressUpcomingDays)
        renderDateFormat(dateFormat, language)
        renderTabletColumns(tabletColumns)

        if (restartApp) restartApp()
        // A theme lands while the Activity is being created, so the change needs a fresh one.
        // Consumed rather than read: recreate() is a configuration change, so this ViewModel outlives it and a flag left standing would recreate on every render.
        if (recreateActivity?.consume() == true) requireActivity().recreate()
      }
    }
  }

  private fun renderSettings(settings: Settings?) {
    if (settings == null) return
    with(binding) {
      settingsIncludeSpecialsSwitch.isChecked = settings.specialSeasonsEnabled
    }
  }

  private fun renderLanguage(language: AppLanguage) {
    with(binding) {
      settingsLanguageValue.setText(language.displayName)
      settingsLanguage.onClick { showLanguageDialog(language) }
    }
  }

  private fun renderTheme(
    theme: AppTheme,
    amoled: Boolean,
  ) {
    with(binding) {
      settingsThemeValue.setText(theme.displayName)
      settingsTheme.onClick { showThemeDialog(theme) }

      // AMOLED only has something to act on where the theme can end up dark.
      // The switch still shows the stored value while it is disabled, so a trip through Light and back leaves the choice intact.
      val canBeDark = theme.canBeDark
      settingsAmoled.isEnabled = canBeDark
      settingsAmoledSwitch.isEnabled = canBeDark
      settingsAmoled.alpha = if (canBeDark) 1F else DISABLED_ALPHA
      settingsAmoledSwitch.isChecked = amoled
      settingsAmoled.onClick { if (canBeDark) viewModel.setAmoled(!amoled, requireAppContext()) }
    }
  }

  private fun renderTabletColumns(columns: Int) {
    with(binding) {
      settingsTabletColumnsValue.text = String.format(Locale.getDefault(), "%d", columns)
      settingsTabletColumns.onClick {
        showTabletColumnsDialog(columns)
      }
    }
  }

  private fun renderCountry(country: AppCountry?) {
    if (country == null) return
    with(binding) {
      settingsCountryValue.text = country.displayName
      settingsCountry.onClick { showCountryDialog(country) }
    }
  }

  private fun renderProgressUpcoming(progressUpcomingDays: Long?) {
    if (progressUpcomingDays == null) return
    with(binding) {
      settingsUpcomingValue.text =
        if (progressUpcomingDays > 0L) {
          resources.getQuantityString(R.plurals.textDays, progressUpcomingDays.toInt(), progressUpcomingDays)
        } else {
          getString(R.string.textDisabled)
        }
      settingsUpcomingSection.onClick {
        showProgressUpcomingDialog(progressUpcomingDays)
      }
    }
  }

  private fun renderProgressType(type: ProgressNextEpisodeType?) {
    if (type == null) return
    with(binding) {
      settingsProgressNextValue.text =
        when (type) {
          LAST_WATCHED -> getString(R.string.textNextEpisodeLastWatched)
          OLDEST -> getString(R.string.textNextEpisodeOldest)
        }
      settingsProgressNext.onClick { showProgressTypeDialog(type) }
    }
  }

  private fun renderDateSelection(type: ProgressDateSelectionType?) {
    if (type == null) return
    with(binding) {
      settingsDateSelectionValue.text =
        when (type) {
          ALWAYS_ASK -> getString(R.string.textDateSelectionAsk)
          NOW -> getString(R.string.textDateSelectionNow)
        }
      settingsDateSelection.onClick { showDateSelectionTypeDialog(type) }
    }
  }

  private fun renderDateFormat(
    format: AppDateFormat?,
    language: AppLanguage,
  ) {
    if (format == null) return
    with(binding) {
      settingsDateFormatValue.text =
        DateFormatProvider
          .loadSettingsFormat(format, language.code)
          .format(nowUtc().toLocalZone())
      settingsDateFormat.onClick { showDateFormatDialog(format, language) }
    }
  }

  private fun showThemeDialog(theme: AppTheme) =
    showSingleChoiceModal(AppTheme.supported(), theme, { getString(it.displayName) }) {
      if (it != theme) viewModel.setTheme(it, requireAppContext())
    }

  private fun showLanguageDialog(language: AppLanguage) =
    showSingleChoiceModal(AppLanguage.entries, language, { getString(it.displayName) }) {
      if (it != language) viewModel.setLanguage(it)
    }

  private fun showProgressUpcomingDialog(days: Long) {
    val options = Config.PROGRESS_UPCOMING_OPTIONS
    val selected = options.firstOrNull { it.toLong() == days }

    showSingleChoiceModal(options, selected, {
      if (it == 0) getString(R.string.textDisabled) else resources.getQuantityString(R.plurals.textDays, it, it)
    }) {
      if (it != selected) viewModel.setProgressUpcomingDays(it.toLong())
    }
  }

  private fun showTabletColumnsDialog(columns: Int) =
    showSingleChoiceModal(listOf(1, 2), columns, { String.format(Locale.getDefault(), "%d", it) }) {
      if (it != columns) viewModel.setTabletColumns(it)
    }

  private fun showCountryDialog(country: AppCountry) =
    showSingleChoiceModal(AppCountry.entries, country, { it.displayName }) {
      if (it != country) viewModel.setCountry(it)
    }

  private fun showProgressTypeDialog(type: ProgressNextEpisodeType) =
    showSingleChoiceModal(ProgressNextEpisodeType.entries, type, {
      getString(
        when (it) {
          LAST_WATCHED -> R.string.textNextEpisodeLastWatched
          OLDEST -> R.string.textNextEpisodeOldest
        },
      )
    }) {
      if (it != type) viewModel.setProgressType(it)
    }

  private fun showDateSelectionTypeDialog(type: ProgressDateSelectionType) =
    showSingleChoiceModal(ProgressDateSelectionType.entries, type, {
      getString(
        when (it) {
          ALWAYS_ASK -> R.string.textDateSelectionAsk
          NOW -> R.string.textDateSelectionNow
        },
      )
    }) {
      if (it != type) viewModel.setDateSelectionType(it)
    }

  private fun showDateFormatDialog(
    format: AppDateFormat,
    language: AppLanguage,
  ) = showSingleChoiceModal(
    options = AppDateFormat.entries,
    selected = format,
    label = { DateFormatProvider.loadSettingsFormat(it, language.code).format(nowUtc().toLocalZone()) },
    // These labels are rendered dates, long enough to need their own size.
    textSizeSp = 13F,
  ) {
    if (it != format) viewModel.setDateFormat(it, requireAppContext())
  }

  private fun showRecentShowsDialog(settings: Settings?) {
    if (settings == null) return

    showSingleChoiceModal(Config.MY_SHOWS_RECENTS_OPTIONS, settings.myRecentsAmount, { it.toString() }) {
      viewModel.setRecentShowsAmount(it)
    }
  }

  private fun restartApp() {
    try {
      ProcessPhoenix.triggerRebirth(requireAppContext())
    } catch (error: Throwable) {
      Runtime.getRuntime().exit(0)
    }
  }
}
