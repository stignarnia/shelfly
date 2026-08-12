package xyz.stignarnia.ui_movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.errors.ErrorHelper
import xyz.stignarnia.common.errors.ShelflyError.CoroutineCancellation
import xyz.stignarnia.common.errors.ShelflyError.ResourceNotFoundError
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toUtcZone
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_base.notifications.AnnouncementManager
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.combine
import xyz.stignarnia.ui_base.utilities.extensions.launchDelayed
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.ui_model.RatingState
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.UserRating
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_movie.MovieDetailsEvent.Finish
import xyz.stignarnia.ui_movie.MovieDetailsEvent.RequestWidgetsUpdate
import xyz.stignarnia.ui_movie.MovieDetailsUiState.FollowedState
import xyz.stignarnia.ui_movie.cases.MovieDetailsHiddenCase
import xyz.stignarnia.ui_movie.cases.MovieDetailsListsCase
import xyz.stignarnia.ui_movie.cases.MovieDetailsMainCase
import xyz.stignarnia.ui_movie.cases.MovieDetailsMyMoviesCase
import xyz.stignarnia.ui_movie.cases.MovieDetailsTranslationCase
import xyz.stignarnia.ui_movie.cases.MovieDetailsWatchlistCase
import xyz.stignarnia.ui_movie.helpers.MovieDetailsMeta
import xyz.stignarnia.ui_movie.sections.ratings.cases.MovieDetailsRatingCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
  private val mainCase: MovieDetailsMainCase,
  private val translationCase: MovieDetailsTranslationCase,
  private val myMoviesCase: MovieDetailsMyMoviesCase,
  private val ratingsCase: MovieDetailsRatingCase,
  private val watchlistCase: MovieDetailsWatchlistCase,
  private val hiddenCase: MovieDetailsHiddenCase,
  private val listsCase: MovieDetailsListsCase,
  private val settingsRepository: SettingsRepository,
  private val imagesProvider: MovieImagesProvider,
  private val dateFormatProvider: DateFormatProvider,
  private val announcementManager: AnnouncementManager,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private var movie by notNull<Movie>()

  private val movieState = MutableStateFlow<Movie?>(null)
  private val movieLoadingState = MutableStateFlow<Boolean?>(null)
  private val imageState = MutableStateFlow<Image?>(null)
  private val followedState = MutableStateFlow<FollowedState?>(null)
  private val ratingState = MutableStateFlow<RatingState?>(null)
  private val translationState = MutableStateFlow<Translation?>(null)
  private val metaState = MutableStateFlow<MovieDetailsMeta?>(null)
  private val spoilersState = MutableStateFlow<SpoilersSettings?>(null)
  private val listsCountState = MutableStateFlow(0)

  val parentMovieState = movieState.asStateFlow()
  val parentFollowedState = followedState.asStateFlow()

  fun loadDetails(id: IdTmdb) {
    viewModelScope.launch {
      val progressJob = launchDelayed(700) {
        movieLoadingState.value = true
      }
      try {
        movie = mainCase.loadDetails(id)

        val isMyMovie = async { myMoviesCase.getMyMovie(movie) }
        val isWatchlist = async { watchlistCase.isWatchlist(movie) }
        val isHidden = async { hiddenCase.isHidden(movie) }

        val myMovie = isMyMovie.await()
        val isFollowed = FollowedState(
          isMyMovie = myMovie != null,
          isWatchlist = isWatchlist.await(),
          isHidden = isHidden.await(),
          withAnimation = false,
          watchedAt = myMovie?.updatedAt?.let { dateFromMillis(it) },
        )

        progressJob.cancel()

        movieState.value = movie
        movieLoadingState.value = false
        followedState.value = isFollowed
        ratingState.value = RatingState(rateLoading = false)
        spoilersState.value = settingsRepository.spoilers.getAll()
        metaState.value = MovieDetailsMeta(
          dateFormat = dateFormatProvider.loadShortDayFormat(),
          commentsDateFormat = dateFormatProvider.loadFullHourFormat(),
          watchedAtDateFormat = dateFormatProvider.loadFullHourFormat(),
          isSignedIn = false,
        )

        loadBackgroundImage(movie)
        loadListsCount(movie)
        loadUserRating()
        loadTranslation()

        eventChannel.send(RequestWidgetsUpdate)
      } catch (error: Throwable) {
        Timber.e(error)
        progressJob.cancel()
        when (ErrorHelper.parse(error)) {
          is CoroutineCancellation -> {
            rethrowCancellation(error)
          }
          is ResourceNotFoundError -> {
            // Malformed catalog data or duplicate entry.
            messageChannel.send(MessageEvent.Info(R.string.errorMalformedMovie))
          }
          else -> {
            messageChannel.send(MessageEvent.Error(R.string.errorCouldNotLoadMovie))
          }
        }
      }
    }
  }

  private fun loadBackgroundImage(movie: Movie? = null) {
    viewModelScope.launch {
      try {
        val backgroundImage = imagesProvider.loadRemoteImage(
          movie ?: this@MovieDetailsViewModel.movie,
          ImageType.FANART,
        )
        imageState.value = backgroundImage
      } catch (error: Throwable) {
        imageState.value = Image.createUnavailable(ImageType.FANART)
        Timber.e(error)
        rethrowCancellation(error)
      }
    }
  }

  private fun loadTranslation() {
    viewModelScope.launch {
      try {
        translationCase.loadTranslation(movie)?.let {
          translationState.value = it
        }
      } catch (error: Throwable) {
        rethrowCancellation(error)
      }
    }
  }

  fun loadListsCount(movie: Movie? = null) {
    viewModelScope.launch {
      val count = listsCase.countLists(movie ?: this@MovieDetailsViewModel.movie)
      listsCountState.value = count
    }
  }

  fun loadUserRating() {
    viewModelScope.launch {
      try {
        ratingState.value = RatingState(rateLoading = true)
        val rating = ratingsCase.loadRating(movie)
        ratingState.value =
          RatingState(rateLoading = false, userRating = rating ?: UserRating.EMPTY)
      } catch (error: Throwable) {
        ratingState.value = RatingState(rateLoading = false)
        rethrowCancellation(error)
      }
    }
  }

  fun addToMyMovies(
    isCustomDateSelected: Boolean = false,
    customDate: ZonedDateTime? = null,
  ) {
    viewModelScope.launch {
      if (!isCustomDateSelected && settingsRepository.progressDateSelectionType == ALWAYS_ASK) {
        eventChannel.send(MovieDetailsEvent.OpenDateSelectionSheet(movie))
        return@launch
      }
      myMoviesCase.addToMyMovies(movie, customDate)
      followedState.value = FollowedState
        .inMyMovies()
        .copy(watchedAt = customDate?.toUtcZone() ?: nowUtc())
      eventChannel.send(RequestWidgetsUpdate)
    }
  }

  fun addToWatchlist() {
    viewModelScope.launch {
      watchlistCase.addToWatchlist(movie)
      followedState.value = FollowedState.inWatchlist()
      eventChannel.send(RequestWidgetsUpdate)
    }
  }

  fun addToHidden() {
    viewModelScope.launch {
      hiddenCase.addToHidden(movie)
      followedState.value = FollowedState.inHidden()
      eventChannel.send(RequestWidgetsUpdate)
    }
  }

  fun removeFromMyMovies() {
    viewModelScope.launch {
      val isMyMovie = myMoviesCase.getMyMovie(movie) != null
      val isWatchlist = watchlistCase.isWatchlist(movie)
      val isHidden = hiddenCase.isHidden(movie)

      when {
        isMyMovie -> myMoviesCase.removeFromMyMovies(movie)
        isWatchlist -> watchlistCase.removeFromWatchlist(movie)
        isHidden -> hiddenCase.removeFromHidden(movie)
      }

      val state = FollowedState.idle()
      when {
        isMyMovie -> {
          followedState.value = state
        }
        isWatchlist -> {
          followedState.value = state
        }
        isHidden -> {
          followedState.value = state
        }
        else -> {
          error("Unexpected movie state.")
        }
      }
      eventChannel.send(RequestWidgetsUpdate)
      announcementManager.refreshMoviesAnnouncements()
    }
  }

  fun removeMalformedMovie(id: IdTmdb) {
    viewModelScope.launch {
      try {
        mainCase.removeMalformedMovie(id)
      } catch (error: Throwable) {
        Timber.e(error)
        rethrowCancellation(error)
      } finally {
        eventChannel.send(Finish)
      }
    }
  }

  val uiState = combine(
    movieState,
    movieLoadingState,
    imageState,
    followedState,
    ratingState,
    translationState,
    listsCountState,
    metaState,
    spoilersState,
  ) { s1, s2, s3, s4, s5, s6, s7, s8, s9 ->
    MovieDetailsUiState(
      movie = s1,
      movieLoading = s2,
      image = s3,
      followedState = s4,
      ratingState = s5,
      translation = s6,
      listsCount = s7,
      meta = s8,
      spoilers = s9,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = MovieDetailsUiState(),
  )
}
