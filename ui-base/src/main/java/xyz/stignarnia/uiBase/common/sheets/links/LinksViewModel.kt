package xyz.stignarnia.uiBase.common.sheets.links

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.common.AppCountry
import javax.inject.Inject

@HiltViewModel
class LinksViewModel
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) : ViewModel() {
    fun loadCountry() = AppCountry.fromCode(settingsRepository.country)
  }
