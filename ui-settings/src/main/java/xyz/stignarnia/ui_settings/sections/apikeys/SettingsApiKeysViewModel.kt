package xyz.stignarnia.ui_settings.sections.apikeys

import androidx.lifecycle.ViewModel
import xyz.stignarnia.data_remote.apikey.ApiKeyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsApiKeysViewModel @Inject constructor(
  private val apiKeyProvider: ApiKeyProvider,
) : ViewModel() {

  private val state = MutableStateFlow(SettingsApiKeysUiState())
  val uiState = state.asStateFlow()

  fun refresh() {
    state.value =
      SettingsApiKeysUiState(
        tmdbApiKey = apiKeyProvider.getTmdbApiKey(),
        omdbApiKey = apiKeyProvider.getOmdbApiKey(),
      )
  }

  fun setTmdbApiKey(key: String) {
    apiKeyProvider.setTmdbApiKey(key)
    refresh()
  }

  fun setOmdbApiKey(key: String) {
    apiKeyProvider.setOmdbApiKey(key)
    refresh()
  }
}
