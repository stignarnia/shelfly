package xyz.stignarnia.ui_base.common.sheets.ratings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.errors.ErrorHelper
import xyz.stignarnia.common.errors.ShelflyError.CoroutineCancellation
import xyz.stignarnia.common.errors.ShelflyError.UnauthorizedError
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.common.sheets.ratings.RatingsBottomSheet.Options
import xyz.stignarnia.ui_base.common.sheets.ratings.RatingsBottomSheet.Options.Operation
import xyz.stignarnia.ui_base.common.sheets.ratings.RatingsBottomSheet.Options.Type
import xyz.stignarnia.ui_base.common.sheets.ratings.cases.RatingsEpisodeCase
import xyz.stignarnia.ui_base.common.sheets.ratings.cases.RatingsMovieCase
import xyz.stignarnia.ui_base.common.sheets.ratings.cases.RatingsSeasonCase
import xyz.stignarnia.ui_base.common.sheets.ratings.cases.RatingsShowCase
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.UserRating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RatingsSheetViewModel @Inject constructor(
  private val showRatingsCase: RatingsShowCase,
  private val movieRatingsCase: RatingsMovieCase,
  private val episodeRatingsCase: RatingsEpisodeCase,
  private val seasonRatingsCase: RatingsSeasonCase,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val loadingState = MutableStateFlow(false)
  private val ratingState = MutableStateFlow<UserRating?>(null)

  fun loadRating(options: Options) {
    viewModelScope.launch {
      try {
        val rating = with(options) {
          when (type) {
            Type.SHOW -> showRatingsCase.loadRating(id)
            Type.MOVIE -> movieRatingsCase.loadRating(id)
            Type.EPISODE -> episodeRatingsCase.loadRating(requireShowId(), seasonNumber(), episodeNumber())
            Type.SEASON -> seasonRatingsCase.loadRating(requireShowId(), seasonNumber())
          }
        }
        ratingState.value = rating
      } catch (error: Throwable) {
        handleError(error)
      }
    }
  }

  fun saveRating(
    rating: Int,
    options: Options,
  ) {
    viewModelScope.launch {
      try {
        loadingState.value = true
        with(options) {
          when (type) {
            Type.SHOW -> showRatingsCase.saveRating(id, rating)
            Type.MOVIE -> movieRatingsCase.saveRating(id, rating)
            Type.EPISODE ->
              episodeRatingsCase.saveRating(requireShowId(), seasonNumber(), episodeNumber(), rating)
            Type.SEASON -> seasonRatingsCase.saveRating(requireShowId(), seasonNumber(), rating)
          }
        }
        eventChannel.send(FinishUiEvent(operation = Operation.SAVE))
      } catch (error: Throwable) {
        loadingState.value = false
        handleError(error)
      }
    }
  }

  fun removeRating(options: Options) {
    viewModelScope.launch {
      try {
        loadingState.value = true
        with(options) {
          when (type) {
            Type.SHOW -> showRatingsCase.deleteRating(id)
            Type.MOVIE -> movieRatingsCase.deleteRating(id)
            Type.EPISODE -> episodeRatingsCase.deleteRating(requireShowId(), seasonNumber(), episodeNumber())
            Type.SEASON -> seasonRatingsCase.deleteRating(requireShowId(), seasonNumber())
          }
        }
        eventChannel.send(FinishUiEvent(operation = Operation.REMOVE))
      } catch (error: Throwable) {
        loadingState.value = false
        handleError(error)
      }
    }
  }

  // A season or episode rating is stored under its show, so the sheet cannot be
  // opened for one without saying which show it belongs to.

  private fun Options.requireShowId(): IdTmdb = checkNotNull(showId) { "A $type rating needs the show it belongs to." }

  private fun Options.seasonNumber(): Int = checkNotNull(seasonNumber) { "A $type rating needs a season number." }

  private fun Options.episodeNumber(): Int = checkNotNull(episodeNumber) { "A $type rating needs an episode number." }

  private suspend fun handleError(error: Throwable) {
    when (ErrorHelper.parse(error)) {
      is CoroutineCancellation -> throw error
      is UnauthorizedError -> messageChannel.send(MessageEvent.Error(R.string.errorGeneral))
      else -> messageChannel.send(MessageEvent.Error(R.string.errorGeneral))
    }
  }

  val uiState = combine(
    loadingState,
    ratingState,
  ) { s1, s2 ->
    RatingsUiState(
      isLoading = s1,
      rating = s2,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = RatingsUiState(),
  )
}
