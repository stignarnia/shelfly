package xyz.stignarnia.ui_my_movies.filters.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_my_movies.filters.CollectionFiltersOrigin
import xyz.stignarnia.ui_my_movies.filters.CollectionFiltersOrigin.HIDDEN_MOVIES
import xyz.stignarnia.ui_my_movies.filters.CollectionFiltersOrigin.MY_MOVIES
import xyz.stignarnia.ui_my_movies.filters.CollectionFiltersOrigin.WATCHLIST_MOVIES
import xyz.stignarnia.ui_my_movies.filters.CollectionFiltersUiEvent.ApplyFilters
import xyz.stignarnia.ui_my_movies.filters.CollectionFiltersUiEvent.CloseFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CollectionFiltersGenreViewModel @Inject constructor(
  private val settingsRepository: SettingsRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val genresState = MutableStateFlow<List<Genre>?>(null)
  private val loadingState = MutableStateFlow(false)

  private lateinit var origin: CollectionFiltersOrigin

  fun loadData(origin: CollectionFiltersOrigin) {
    this.origin = origin
    genresState.value = when (origin) {
      MY_MOVIES -> settingsRepository.filters.myMoviesGenres
      WATCHLIST_MOVIES -> settingsRepository.filters.watchlistMoviesGenres
      HIDDEN_MOVIES -> settingsRepository.filters.hiddenMoviesGenres
    }.toList()
  }

  fun saveGenres(genres: List<Genre>) {
    viewModelScope.launch {
      if (genres == genresState.value) {
        eventChannel.send(CloseFilters)
        return@launch
      }
      when (origin) {
        MY_MOVIES -> settingsRepository.filters.myMoviesGenres = genres
        WATCHLIST_MOVIES -> settingsRepository.filters.watchlistMoviesGenres = genres
        HIDDEN_MOVIES -> settingsRepository.filters.hiddenMoviesGenres = genres
      }
      eventChannel.send(ApplyFilters)
    }
  }

  val uiState = combine(
    genresState,
    loadingState,
  ) { s1, s2 ->
    CollectionFiltersGenreUiState(
      genres = s1,
      isLoading = s2,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = CollectionFiltersGenreUiState(),
  )
}
