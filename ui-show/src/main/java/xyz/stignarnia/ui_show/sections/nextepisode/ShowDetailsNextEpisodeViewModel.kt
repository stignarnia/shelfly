package xyz.stignarnia.ui_show.sections.nextepisode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_show.sections.nextepisode.cases.ShowDetailsNextEpisodeCase
import xyz.stignarnia.ui_show.sections.nextepisode.cases.ShowDetailsTranslationCase
import xyz.stignarnia.ui_show.sections.nextepisode.cases.ShowDetailsWatchedCase
import xyz.stignarnia.ui_show.sections.nextepisode.helpers.NextEpisodeBundle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowDetailsNextEpisodeViewModel @Inject constructor(
  private val nextEpisodeCase: ShowDetailsNextEpisodeCase,
  private val translationCase: ShowDetailsTranslationCase,
  private val watchedCase: ShowDetailsWatchedCase,
  private val spoilersSettingsRepository: SettingsSpoilersRepository,
  private val dateFormatProvider: DateFormatProvider,
) : ViewModel() {

  private lateinit var show: Show

  private val nextEpisodeState = MutableStateFlow<NextEpisodeBundle?>(null)
  private val spoilersState = MutableStateFlow<SpoilersSettings?>(null)

  fun loadNextEpisode(show: Show) {
    if (this::show.isInitialized) return
    this.show = show
    viewModelScope.launch {
      try {
        val dateFormat = dateFormatProvider.loadFullHourFormat()
        val episode = nextEpisodeCase.loadNextEpisode(show.ids.tmdb)
        episode?.let {
          val isWatched = watchedCase.isWatched(show, it)
          val nextEpisode = NextEpisodeBundle(
            nextEpisode = Pair(show, it),
            dateFormat = dateFormat,
            isWatched = isWatched,
          )
          spoilersState.value = spoilersSettingsRepository.getAll()
          nextEpisodeState.value = nextEpisode

          val translation = translationCase.loadTranslation(episode, show)
          if (translation?.title?.isNotBlank() == true) {
            val translated = it.copy(title = translation.title)
            val nextEpisodeTranslated = NextEpisodeBundle(
              nextEpisode = Pair(show, translated),
              dateFormat = dateFormat,
              isWatched = isWatched,
            )
            nextEpisodeState.value = nextEpisodeTranslated
          }
        }
      } catch (error: Throwable) {
        rethrowCancellation(error)
      }
    }
  }

  val uiState = combine(
    nextEpisodeState,
    spoilersState,
  ) { s1, s2 ->
    ShowDetailsNextEpisodeUiState(
      nextEpisode = s1,
      spoilersSettings = s2,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = ShowDetailsNextEpisodeUiState(),
  )
}
