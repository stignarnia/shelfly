package xyz.stignarnia.uiSettings.sections.apikeys

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider
import javax.inject.Inject

@HiltViewModel
class SettingsApiKeysViewModel
  @Inject
  constructor(
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
