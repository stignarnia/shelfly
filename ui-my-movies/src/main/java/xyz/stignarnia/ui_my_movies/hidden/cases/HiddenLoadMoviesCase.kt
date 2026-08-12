package xyz.stignarnia.ui_my_movies.hidden.cases

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_base.utilities.extensions.removeDiacritics
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.UserRating
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_model.UpcomingFilter
import xyz.stignarnia.ui_my_movies.common.helpers.CollectionItemSorter
import xyz.stignarnia.ui_my_movies.common.recycler.CollectionListItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@ViewModelScoped
class HiddenLoadMoviesCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsCase: HiddenRatingsCase,
  private val sorter: CollectionItemSorter,
  private val moviesRepository: MoviesRepository,
  private val translationsRepository: TranslationsRepository,
  private val dateFormatProvider: DateFormatProvider,
  private val imagesProvider: MovieImagesProvider,
  private val settingsRepository: SettingsRepository,
) {

  suspend fun loadMovies(searchQuery: String): List<CollectionListItem> =
    withContext(dispatchers.IO) {
      val language = translationsRepository.getLanguage()
      val ratings = ratingsCase.loadRatings()
      val dateFormat = dateFormatProvider.loadShortDayFormat()
      val fullDateFormat = dateFormatProvider.loadFullDayFormat()
      val translations =
        if (language == Config.DEFAULT_LANGUAGE) {
          emptyMap()
        } else {
          translationsRepository.loadAllMoviesLocal(language)
        }
      val spoilersSettings = settingsRepository.spoilers.getAll()

      val sortOrder = settingsRepository.sorting.hiddenMoviesSortOrder
      val sortType = settingsRepository.sorting.hiddenMoviesSortType
      val genres = settingsRepository.filters.hiddenMoviesGenres

      val moviesItems = moviesRepository.hiddenMovies
        .loadAll()
        .map {
          toListItemAsync(
            movie = it,
            translation = translations[it.tmdbId],
            userRating = ratings[it.ids.tmdb],
            dateFormat = dateFormat,
            fullDateFormat = fullDateFormat,
            sortOrder = sortOrder,
            spoilers = spoilersSettings,
          )
        }.awaitAll()
        .filterByQuery(searchQuery)
        .filterByGenre(genres.map { it.slug.lowercase() })
        .sortedWith(sorter.sort(sortOrder, sortType))

      val filtersItem = loadFiltersItem(
        sortOrder = sortOrder,
        sortType = sortType,
        genres = genres,
        count = moviesItems.size,
      )

      if (moviesItems.isNotEmpty() || filtersItem.hasActiveFilters()) {
        listOf(filtersItem) + moviesItems
      } else {
        moviesItems
      }
    }

  private fun loadFiltersItem(
    sortOrder: SortOrder,
    sortType: SortType,
    genres: List<Genre>,
    count: Int,
  ): CollectionListItem.FiltersItem =
    CollectionListItem.FiltersItem(
      sortOrder = sortOrder,
      sortType = sortType,
      genres = genres,
      upcoming = UpcomingFilter.OFF,
      count = count,
    )

  private fun List<CollectionListItem.MovieItem>.filterByQuery(query: String) =
    this.filter {
      it.movie.title
        .removeDiacritics()
        .contains(query, true) ||
        it.translation
          ?.title
          ?.removeDiacritics()
          ?.contains(query, true) == true
    }

  private fun List<CollectionListItem.MovieItem>.filterByGenre(genres: List<String>) =
    filter { genres.isEmpty() || it.movie.genres.any { genre -> genre.lowercase() in genres } }

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
    val image = imagesProvider.findCachedImage(movie, ImageType.POSTER)
    CollectionListItem.MovieItem(
      isLoading = false,
      movie = movie,
      image = image,
      dateFormat = dateFormat,
      fullDateFormat = fullDateFormat,
      translation = translation,
      sortOrder = sortOrder,
      userRating = userRating?.rating,
      spoilers = CollectionListItem.MovieItem.Spoilers(
        isSpoilerHidden = spoilers.isHiddenMoviesHidden,
        isSpoilerRatingsHidden = spoilers.isHiddenMoviesRatingsHidden,
        isSpoilerTapToReveal = spoilers.isTapToReveal,
      ),
    )
  }
}
