package xyz.stignarnia.uiProgressMovies.progress.cases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.extensions.removeDiacritics
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiProgressMovies.helpers.ProgressMoviesItemsSorter
import xyz.stignarnia.uiProgressMovies.progress.recycler.ProgressMovieListItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressMoviesItemsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val moviesRepository: MoviesRepository,
    private val translationsRepository: TranslationsRepository,
    private val ratingsRepository: RatingsRepository,
    private val settingsRepository: SettingsRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val imagesProvider: MovieImagesProvider,
    private val dateFormatProvider: DateFormatProvider,
    private val sorter: ProgressMoviesItemsSorter,
  ) {
    suspend fun loadItems(searchQuery: String) =
      withContext(dispatchers.IO) {
        val language = translationsRepository.getLanguage()
        val dateFormat = dateFormatProvider.loadFullDayFormat()
        val spoilers = settingsRepository.spoilers.getAll()

        val sortOrder = settingsRepository.sorting.progressMoviesSortOrder
        val sortType = settingsRepository.sorting.progressMoviesSortType

        val watchlistMovies = moviesRepository.watchlistMovies.loadAll()
        val items =
          watchlistMovies
            .map { movie ->
              async {
                val rating = ratingsRepository.movies.loadRatings(listOf(movie))
                var translation: Translation? = null
                if (language != Config.DEFAULT_LANGUAGE) {
                  translation = translationsRepository.loadTranslation(movie, language, onlyLocal = true)
                }

                ProgressMovieListItem.MovieItem(
                  movie = movie,
                  image = imagesProvider.findCachedImage(movie, ImageType.POSTER),
                  isLoading = false,
                  isPinned = pinnedItemsRepository.isItemPinned(movie),
                  translation = translation,
                  dateFormat = dateFormat,
                  sortOrder = sortOrder,
                  userRating = rating.firstOrNull()?.rating,
                  spoilers = spoilers,
                )
              }
            }.awaitAll()

        val filtered = filterItems(searchQuery, items)
        val sorted = filtered.sortedWith(sorter.sort(sortOrder, sortType))
        val preparedItems = prepareItems(sorted)

        if (preparedItems.isNotEmpty()) {
          val filtersItem = loadFiltersItem(sortOrder, sortType)
          listOf(filtersItem) + preparedItems
        } else {
          preparedItems
        }
      }

    private fun loadFiltersItem(
      sortOrder: SortOrder,
      sortType: SortType,
    ): ProgressMovieListItem.FiltersItem =
      ProgressMovieListItem.FiltersItem(
        sortOrder = sortOrder,
        sortType = sortType,
      )

    private fun filterItems(
      query: String,
      items: List<ProgressMovieListItem.MovieItem>,
    ) = items.filter {
      it.movie.title
        .removeDiacritics()
        .contains(query, true) ||
        it.translation
          ?.title
          ?.removeDiacritics()
          ?.contains(query, true) == true
    }

    private fun prepareItems(items: List<ProgressMovieListItem.MovieItem>) =
      items
        .asSequence()
        .filter { !it.movie.hasNoDate() }
        .filter { it.movie.released == null || it.movie.hasAired() }
        .sortedByDescending { it.isPinned }
        .toList()
  }
