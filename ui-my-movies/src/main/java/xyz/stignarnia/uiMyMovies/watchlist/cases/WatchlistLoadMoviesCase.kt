package xyz.stignarnia.uiMyMovies.watchlist.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiMyMovies.common.helpers.CollectionItemFilter
import xyz.stignarnia.uiMyMovies.common.helpers.CollectionItemSorter
import xyz.stignarnia.uiMyMovies.common.recycler.CollectionListItem
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@ViewModelScoped
class WatchlistLoadMoviesCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsCase: WatchlistRatingsCase,
    private val sorter: CollectionItemSorter,
    private val filters: CollectionItemFilter,
    private val moviesRepository: MoviesRepository,
    private val translationsRepository: TranslationsRepository,
    private val dateFormatProvider: DateFormatProvider,
    private val imagesProvider: MovieImagesProvider,
    private val settingsRepository: SettingsRepository,
  ) {
    suspend fun loadMovies(searchQuery: String): List<CollectionListItem> =
      withContext(dispatchers.IO) {
        val ratings = ratingsCase.loadRatings()
        val dateFormat = dateFormatProvider.loadShortDayFormat()
        val fullDateFormat = dateFormatProvider.loadFullDayFormat()
        val language = translationsRepository.getLanguage()
        val spoilers = settingsRepository.spoilers.getAll()
        val translations =
          if (language == Config.DEFAULT_LANGUAGE) {
            emptyMap()
          } else {
            translationsRepository.loadAllMoviesLocal(language)
          }

        var filtersItem = loadFiltersItem()
        val filtersGenres = filtersItem.genres.map { it.slug.lowercase() }

        val moviesItems =
          moviesRepository.watchlistMovies
            .loadAll()
            .map {
              toListItemAsync(
                movie = it,
                translation = translations[it.tmdbId],
                userRating = ratings[it.ids.tmdb],
                dateFormat = dateFormat,
                fullDateFormat = fullDateFormat,
                sortOrder = filtersItem.sortOrder,
                spoilers = spoilers,
              )
            }.awaitAll()
            .filter {
              filters.filterByQuery(it, searchQuery) &&
                filters.filterUpcoming(it, filtersItem.upcoming) &&
                filters.filterGenres(it, filtersGenres)
            }.sortedWith(sorter.sort(filtersItem.sortOrder, filtersItem.sortType))

        filtersItem = filtersItem.copy(count = moviesItems.size)

        if (moviesItems.isNotEmpty() || filtersItem.hasActiveFilters()) {
          listOf(filtersItem) + moviesItems
        } else {
          moviesItems
        }
      }

    private fun loadFiltersItem(): CollectionListItem.FiltersItem =
      CollectionListItem.FiltersItem(
        sortOrder = settingsRepository.sorting.watchlistMoviesSortOrder,
        sortType = settingsRepository.sorting.watchlistMoviesSortType,
        genres = settingsRepository.filters.watchlistMoviesGenres,
        upcoming = settingsRepository.filters.watchlistMoviesUpcoming,
        count = 0,
      )

    suspend fun loadTranslation(
      movie: Movie,
      onlyLocal: Boolean,
    ): Translation? =
      withContext(dispatchers.IO) {
        val language = translationsRepository.getLanguage()
        if (language == Config.DEFAULT_LANGUAGE) {
          return@withContext Translation.EMPTY
        }
        translationsRepository.loadTranslation(movie, language, onlyLocal)
      }

    private fun CoroutineScope.toListItemAsync(
      movie: Movie,
      translation: Translation?,
      userRating: UserRating?,
      dateFormat: DateTimeFormatter,
      fullDateFormat: DateTimeFormatter,
      sortOrder: SortOrder,
      spoilers: SpoilersSettings,
    ) = async {
      CollectionListItem.MovieItem(
        isLoading = false,
        movie = movie,
        image = imagesProvider.findCachedImage(movie, ImageType.POSTER),
        dateFormat = dateFormat,
        fullDateFormat = fullDateFormat,
        translation = translation,
        userRating = userRating?.rating,
        sortOrder = sortOrder,
        spoilers =
          CollectionListItem.MovieItem.Spoilers(
            isSpoilerHidden = spoilers.isWatchlistMoviesHidden,
            isSpoilerRatingsHidden = spoilers.isWatchlistMoviesRatingsHidden,
            isSpoilerTapToReveal = spoilers.isTapToReveal,
          ),
      )
    }
  }
