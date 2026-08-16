package xyz.stignarnia.ui_settings.sections.general

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.jakewharton.processphoenix.ProcessPhoenix
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.common.AppCountry
import xyz.stignarnia.ui_base.dates.AppDateFormat
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.ProgressDateSelectionType
import xyz.stignarnia.ui_model.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.ui_model.ProgressDateSelectionType.NOW
import xyz.stignarnia.ui_model.ProgressNextEpisodeType
import xyz.stignarnia.ui_model.ProgressNextEpisodeType.LAST_WATCHED
import xyz.stignarnia.ui_model.ProgressNextEpisodeType.OLDEST
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsGeneralBinding
import xyz.stignarnia.ui_settings.helpers.AppLanguage
import xyz.stignarnia.ui_settings.helpers.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsGeneralFragment : BaseFragment<SettingsGeneralViewModel>(R.layout.fragment_settings_general) {

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
        renderTheme(theme)
        renderCountry(country)
        renderProgressType(progressNextType)
        renderDateSelection(progressDateSelectionType)
        renderProgressUpcoming(progressUpcomingDays)
        renderDateFormat(dateFormat, language)
        renderTabletColumns(tabletColumns)

        if (restartApp) restartApp()
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

  private fun renderTheme(theme: AppTheme) {
    with(binding) {
      settingsThemeValue.setText(theme.displayName)
      settingsTheme.onClick { showThemeDialog(theme) }
    }
  }

  private fun renderTabletColumns(columns: Int) {
    with(binding) {
      settingsTabletColumnsValue.text = columns.toString()
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
      settingsUpcomingValue.text = if (progressUpcomingDays > 0L) {
        getString(R.string.textDays, progressUpcomingDays)
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
      settingsProgressNextValue.text = when (type) {
        LAST_WATCHED -> getString(R.string.textNextEpisodeLastWatched)
        OLDEST -> getString(R.string.textNextEpisodeOldest)
      }
      settingsProgressNext.onClick { showProgressTypeDialog(type) }
    }
  }

  private fun renderDateSelection(type: ProgressDateSelectionType?) {
    if (type == null) return
    with(binding) {
      settingsDateSelectionValue.text = when (type) {
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
      settingsDateFormatValue.text = DateFormatProvider
        .loadSettingsFormat(format, language.code)
        .format(nowUtc().toLocalZone())
      settingsDateFormat.onClick { showDateFormatDialog(format, language) }
    }
  }

  private fun showThemeDialog(theme: AppTheme) =
    showSingleChoiceDialog(AppTheme.entries, theme, { getString(it.displayName) }) {
      if (it != theme) viewModel.setTheme(it)
    }

  private fun showLanguageDialog(language: AppLanguage) =
    showSingleChoiceDialog(AppLanguage.entries, language, { getString(it.displayName) }) {
      if (it != language) viewModel.setLanguage(it)
    }

  private fun showProgressUpcomingDialog(days: Long) {
    val options = Config.PROGRESS_UPCOMING_OPTIONS
    val selected = options.firstOrNull { it.toLong() == days }

    showSingleChoiceDialog(options, selected, {
      if (it == 0) getString(R.string.textDisabled) else getString(R.string.textDays, it)
    }) {
      if (it != selected) viewModel.setProgressUpcomingDays(it.toLong())
    }
  }

  private fun showTabletColumnsDialog(columns: Int) =
    showSingleChoiceDialog(listOf(1, 2), columns, { it.toString() }) {
      if (it != columns) viewModel.setTabletColumns(it)
    }

  private fun showCountryDialog(country: AppCountry) =
    showSingleChoiceDialog(AppCountry.entries, country, { it.displayName }) {
      if (it != country) viewModel.setCountry(it)
    }

  private fun showProgressTypeDialog(type: ProgressNextEpisodeType) =
    showSingleChoiceDialog(ProgressNextEpisodeType.entries, type, {
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
    showSingleChoiceDialog(ProgressDateSelectionType.entries, type, {
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
  ) = showSingleChoiceDialog(
    options = AppDateFormat.entries,
    selected = format,
    label = { DateFormatProvider.loadSettingsFormat(it, language.code).format(nowUtc().toLocalZone()) },
    style = R.style.AlertDialog_SmallText,
  ) {
    if (it != format) viewModel.setDateFormat(it, requireAppContext())
  }

  private fun showRecentShowsDialog(settings: Settings?) {
    if (settings == null) return

    showSingleChoiceDialog(Config.MY_SHOWS_RECENTS_OPTIONS, settings.myRecentsAmount, { it.toString() }) {
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
