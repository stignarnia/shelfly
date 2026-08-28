package xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.helpers.MovieContextItem
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.ImageType
import javax.inject.Inject

@ViewModelScoped
class MovieContextMenuLoadItemCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val moviesRepository: MoviesRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val imagesProvider: MovieImagesProvider,
    private val translationsRepository: TranslationsRepository,
    private val ratingsRepository: RatingsRepository,
    private val settingsSpoilersRepository: SettingsSpoilersRepository,
    private val dateFormatProvider: DateFormatProvider,
  ) {
    suspend fun loadItem(tmdbId: IdTmdb) =
      withContext(dispatchers.IO) {
        val movie = moviesRepository.movieDetails.load(tmdbId)
        val dateFormat = dateFormatProvider.loadShortDayFormat()
        val language = translationsRepository.getLanguage()
        val spoilers = settingsSpoilersRepository.getAll()

        val imageAsync = async { imagesProvider.loadRemoteImage(movie, ImageType.POSTER) }
        val translationAsync =
          async { translationsRepository.loadTranslation(movie, language = language, onlyLocal = true) }
        val ratingAsync = async { ratingsRepository.movies.loadRatings(listOf(movie)) }

        val isMyMovieAsync = async { moviesRepository.myMovies.exists(tmdbId) }
        val isWatchlistAsync = async { moviesRepository.watchlistMovies.exists(tmdbId) }
        val isHiddenAsync = async { moviesRepository.hiddenMovies.exists(tmdbId) }

        val isPinnedAsync = async { pinnedItemsRepository.isItemPinned(movie) }

        MovieContextItem(
          movie = movie,
          image = imageAsync.await(),
          translation = translationAsync.await(),
          userRating = ratingAsync.await().firstOrNull()?.rating,
          isMyMovie = isMyMovieAsync.await(),
          isWatchlist = isWatchlistAsync.await(),
          isHidden = isHiddenAsync.await(),
          isPinnedTop = isPinnedAsync.await(),
          dateFormat = dateFormat,
          spoilers = spoilers,
        )
      }
  }
