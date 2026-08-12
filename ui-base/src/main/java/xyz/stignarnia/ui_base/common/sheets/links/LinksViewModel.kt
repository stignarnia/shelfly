package xyz.stignarnia.ui_base.common.sheets.links

import androidx.lifecycle.ViewModel
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.common.AppCountry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LinksViewModel @Inject constructor(
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  fun loadCountry() = AppCountry.fromCode(settingsRepository.country)
}
