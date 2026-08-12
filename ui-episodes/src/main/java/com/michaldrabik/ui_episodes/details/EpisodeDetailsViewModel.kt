package com.michaldrabik.ui_episodes.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaldrabik.common.Config
import com.michaldrabik.repository.RatingsRepository
import com.michaldrabik.repository.TranslationsRepository
import com.michaldrabik.repository.images.EpisodeImagesProvider
import com.michaldrabik.repository.settings.SettingsSpoilersRepository
import com.michaldrabik.ui_base.dates.DateFormatProvider
import com.michaldrabik.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import com.michaldrabik.ui_base.utilities.extensions.combine
import com.michaldrabik.ui_base.viewmodel.ChannelsDelegate
import com.michaldrabik.ui_base.viewmodel.DefaultChannelsDelegate
import com.michaldrabik.ui_episodes.details.cases.EpisodeDetailsSeasonCase
import com.michaldrabik.ui_episodes.details.cases.EpisodeDetailsWatchedCase
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Image
import com.michaldrabik.ui_model.RatingState
import com.michaldrabik.ui_model.SpoilersSettings
import com.michaldrabik.ui_model.Translation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EpisodeDetailsViewModel @Inject constructor(
  settingsSpoilersRepository: SettingsSpoilersRepository,
  private val seasonsCase: EpisodeDetailsSeasonCase,
  private val watchedCase: EpisodeDetailsWatchedCase,
  private val imagesProvider: EpisodeImagesProvider,
  private val dateFormatProvider: DateFormatProvider,
  private val ratingsRepository: RatingsRepository,
  private val translationsRepository: TranslationsRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val imageState = MutableStateFlow<Image?>(null)
  private val imageLoadingState = MutableStateFlow(false)
  private val episodesState = MutableStateFlow<List<Episode>?>(null)
  private val signedInState = MutableStateFlow(false)
  private val ratingState = MutableStateFlow<RatingState?>(null)
  private val translationState = MutableStateFlow<Translation?>(null)
  private val lastWatchedAtState = MutableStateFlow<ZonedDateTime?>(null)
  private val dateFormatState = MutableStateFlow<DateTimeFormatter?>(null)
  private val spoilersState = MutableStateFlow<SpoilersSettings?>(null)

  init {
    dateFormatState.value = dateFormatProvider.loadFullHourFormat()
    spoilersState.value = settingsSpoilersRepository.getAll()
  }

  fun loadLastWatchedAt(
    showTmdbId: IdTmdb,
    episode: Episode,
  ) {
    viewModelScope.launch {
      val lastWatchedAt = watchedCase.getLastWatchedAt(showTmdbId, episode)
      lastWatchedAtState.update { lastWatchedAt }
    }
  }

  fun loadImage(
    showId: IdTmdb,
    episode: Episode,
  ) {
    viewModelScope.launch {
      try {
        imageLoadingState.value = true
        val episodeImage = imagesProvider.loadRemoteImage(showId, episode)
        imageState.value = episodeImage
        imageLoadingState.value = false
      } catch (t: Throwable) {
        imageLoadingState.value = false
      }
    }
  }

  fun loadSeason(
    showTmdbId: IdTmdb,
    episode: Episode,
    seasonEpisodes: IntArray?,
  ) {
    viewModelScope.launch {
      val episodes = seasonsCase.loadSeason(showTmdbId, episode, seasonEpisodes)
      if (episodes.isNotEmpty()) {
        delay(100)
      }
      episodesState.value = episodes
    }
  }

  fun loadTranslation(
    showTmdbId: IdTmdb,
    episode: Episode,
  ) {
    viewModelScope.launch {
      try {
        val language = translationsRepository.getLanguage()
        if (language == Config.DEFAULT_LANGUAGE) {
          return@launch
        }
        val translation = translationsRepository.loadTranslation(episode, showTmdbId, language)
        translation?.let {
          translationState.value = it
        }
      } catch (error: Throwable) {
        Timber.e(error)
      }
    }
  }

  fun loadRatings(episode: Episode) {
    viewModelScope.launch {
      try {
        ratingState.value = RatingState(rateLoading = true)
        val rating = ratingsRepository.shows.loadRating(episode)
        ratingState.value = RatingState(rateLoading = false, userRating = rating)
      } catch (error: Throwable) {
        ratingState.value = RatingState(rateLoading = false)
      }
    }
  }

  val uiState = combine(
    imageState,
    imageLoadingState,
    episodesState,
    signedInState,
    ratingState,
    translationState,
    dateFormatState,
    spoilersState,
    lastWatchedAtState,
  ) { s1, s2, s3, s4, s5, s6, s7, s8, s9 ->
    EpisodeDetailsUiState(
      image = s1,
      isImageLoading = s2,
      episodes = s3,
      isSignedIn = s4,
      rating = s5,
      translation = s6,
      dateFormat = s7,
      spoilers = s8,
      lastWatchedAt = s9,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = EpisodeDetailsUiState(),
  )
}
