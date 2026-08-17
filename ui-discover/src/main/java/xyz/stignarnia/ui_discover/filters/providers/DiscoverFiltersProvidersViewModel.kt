package xyz.stignarnia.ui_discover.filters.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.repository.WatchProvidersRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.common.AppCountry
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_discover.filters.providers.DiscoverFiltersProvidersUiEvent.ApplyFilters
import xyz.stignarnia.ui_discover.filters.providers.DiscoverFiltersProvidersUiEvent.CloseFilters
import xyz.stignarnia.ui_model.StreamingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class DiscoverFiltersProvidersViewModel @Inject constructor(
  private val settingsRepository: SettingsRepository,
  private val watchProvidersRepository: WatchProvidersRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  companion object {
    /**
     * A region publishes upwards of two hundred services, most of them a single distributor's storefront.
     * TMDB ranks them for the region, so the head of that list is the part worth offering as chips.
     */
    private const val PROVIDERS_LIMIT = 50
  }

  private val availableState = MutableStateFlow<List<StreamingProvider>?>(null)
  private val selectedState = MutableStateFlow(emptyList<StreamingProvider>())
  private val regionState = MutableStateFlow("")
  private val loadingState = MutableStateFlow(false)
  private val errorState = MutableStateFlow(false)

  init {
    loadProviders()
  }

  private fun loadProviders() {
    viewModelScope.launch {
      val selected = settingsRepository.filters.discoverShowsProviders
      val country = settingsRepository.country
      selectedState.value = selected
      regionState.value = AppCountry.fromCode(country).displayName
      loadingState.value = true
      try {
        val providers = watchProvidersRepository.loadProviders(
          isMovie = false,
          countryCode = country,
        )
        // A selection made before the cap moved, or before the region changed, still has to appear or the sheet could not clear it.
        val selectedIds = selected.map { it.id }
        availableState.value = (
          providers.take(PROVIDERS_LIMIT) +
            providers.filter { it.id in selectedIds } +
            selected
        ).distinctBy { it.id }
        errorState.value = false
      } catch (error: Throwable) {
        // Offline or a rejected key: the saved selection is still editable.
        availableState.value = selected
        errorState.value = true
        Timber.w(error)
        rethrowCancellation(error)
      } finally {
        loadingState.value = false
      }
    }
  }

  fun saveProviders(providers: List<StreamingProvider>) {
    viewModelScope.launch {
      if (providers.map { it.id }.toSet() == selectedState.value.map { it.id }.toSet()) {
        eventChannel.send(CloseFilters)
        return@launch
      }
      settingsRepository.filters.discoverShowsProviders = providers
      eventChannel.send(ApplyFilters)
    }
  }

  val uiState = combine(
    availableState,
    selectedState,
    regionState,
    loadingState,
    errorState,
  ) { s1, s2, s3, s4, s5 ->
    DiscoverFiltersProvidersUiState(
      available = s1,
      selected = s2,
      regionName = s3,
      isLoading = s4,
      isError = s5,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = DiscoverFiltersProvidersUiState(),
  )
}
