package xyz.stignarnia.uiMyShows.common.filters.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.WatchProvidersRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.StreamingProvider
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersOrigin
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersOrigin.HIDDEN_SHOWS
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersOrigin.MY_SHOWS
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersOrigin.WATCHLIST_SHOWS
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersUiEvent.ApplyFilters
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersUiEvent.CloseFilters
import javax.inject.Inject

@HiltViewModel
internal class CollectionFiltersNetworkViewModel
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
    private val showsRepository: ShowsRepository,
    private val watchProvidersRepository: WatchProvidersRepository,
    private val dispatchers: CoroutineDispatchers,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val availableState = MutableStateFlow<List<StreamingProvider>?>(null)
    private val selectedState = MutableStateFlow(emptyList<String>())
    private val loadingState = MutableStateFlow(false)

    private lateinit var origin: CollectionFiltersOrigin

    fun loadData(origin: CollectionFiltersOrigin) {
      this.origin = origin
      viewModelScope.launch {
        selectedState.value = selectedNetworks()
        loadingState.value = true
        // The offered networks are the ones the collection actually holds rather than a fixed list, matched against region providers for 1:1 badge icons.
        availableState.value =
          withContext(dispatchers.IO) {
            val shows =
              when (origin) {
                MY_SHOWS -> showsRepository.myShows.loadAll()
                WATCHLIST_SHOWS -> showsRepository.watchlistShows.loadAll()
                HIDDEN_SHOWS -> showsRepository.hiddenShows.loadAll()
              }
            val distinctNetworks =
              shows
                .map { it.network }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val country = settingsRepository.country
            val providers =
              runCatching {
                watchProvidersRepository.loadProviders(isMovie = false, countryCode = country)
              }.getOrDefault(emptyList())

            distinctNetworks.map { networkName ->
              val normNet = networkName.filter { it.isLetterOrDigit() }.lowercase()
              val matchedProvider =
                providers.firstOrNull { provider ->
                  val normProv = provider.name.filter { it.isLetterOrDigit() }.lowercase()
                  normNet == normProv ||
                    (normNet.length >= 4 && normProv.contains(normNet)) ||
                    (normProv.length >= 4 && normNet.contains(normProv)) ||
                    (normNet == "hbo" && normProv.contains("max")) ||
                    (normNet == "hbomax" && normProv.contains("max")) ||
                    (normNet.startsWith("disney") && normProv.contains("disney")) ||
                    (normNet.startsWith("apple") && normProv.contains("apple")) ||
                    (normNet.startsWith("amazon") && normProv.contains("amazon")) ||
                    (normNet.startsWith("paramount") && normProv.contains("paramount")) ||
                    (normNet.startsWith("rai") && normProv.contains("rai"))
                }
              StreamingProvider(
                id = matchedProvider?.id ?: networkName.hashCode().toLong(),
                name = networkName,
                logoPath = matchedProvider?.logoPath.orEmpty(),
              )
            }
          }
        loadingState.value = false
      }
    }

    fun saveNetworks(networks: List<String>) {
      viewModelScope.launch {
        if (networks.toSet() == selectedState.value.toSet()) {
          eventChannel.send(CloseFilters)
          return@launch
        }
        when (origin) {
          MY_SHOWS -> settingsRepository.filters.myShowsNetworks = networks
          WATCHLIST_SHOWS -> settingsRepository.filters.watchlistShowsNetworks = networks
          HIDDEN_SHOWS -> settingsRepository.filters.hiddenShowsNetworks = networks
        }
        eventChannel.send(ApplyFilters)
      }
    }

    private fun selectedNetworks() =
      when (origin) {
        MY_SHOWS -> settingsRepository.filters.myShowsNetworks
        WATCHLIST_SHOWS -> settingsRepository.filters.watchlistShowsNetworks
        HIDDEN_SHOWS -> settingsRepository.filters.hiddenShowsNetworks
      }

    val uiState =
      combine(
        availableState,
        selectedState,
        loadingState,
      ) { s1, s2, s3 ->
        CollectionFiltersNetworkUiState(
          available = s1,
          selected = s2,
          isLoading = s3,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = CollectionFiltersNetworkUiState(),
      )
  }
