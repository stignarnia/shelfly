package xyz.stignarnia.uiProgressMovies.calendar.cases.items

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.extensions.removeDiacritics
import xyz.stignarnia.uiModel.CalendarMode.PRESENT_FUTURE
import xyz.stignarnia.uiModel.CalendarMode.RECENTS
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiProgressMovies.calendar.helpers.filters.CalendarFilter
import xyz.stignarnia.uiProgressMovies.calendar.helpers.groupers.CalendarGrouper
import xyz.stignarnia.uiProgressMovies.calendar.helpers.sorter.CalendarSorter
import xyz.stignarnia.uiProgressMovies.calendar.recycler.CalendarMovieListItem

abstract class CalendarMoviesItemsCase constructor(
  private val dispatchers: CoroutineDispatchers,
  private val moviesRepository: MoviesRepository,
  private val translationsRepository: TranslationsRepository,
  private val settingsSpoilersRepository: SettingsSpoilersRepository,
  private val imagesProvider: MovieImagesProvider,
  private val dateFormatProvider: DateFormatProvider,
) {
  abstract val filter: CalendarFilter
  abstract val grouper: CalendarGrouper
  abstract val sorter: CalendarSorter

  suspend fun loadItems(
    searchQuery: String? = "",
    withFilters: Boolean = true,
  ): List<CalendarMovieListItem> =
    withContext(dispatchers.IO) {
      val now = nowUtc().toLocalZone()
      val language = translationsRepository.getLanguage()
      val dateFormat = dateFormatProvider.loadFullDayFormat()
      val spoilers = settingsSpoilersRepository.getAll()

      val (myMovies, watchlistMovies) =
        awaitAll(
          async { moviesRepository.myMovies.loadAll() },
          async { moviesRepository.watchlistMovies.loadAll() },
        )

      val elements =
        (myMovies + watchlistMovies)
          .filter { filter.filter(now, it) }
          .sortedWith(sorter.sort())
          .map { movie ->
            async {
              var translation: Translation? = null
              if (language != Config.DEFAULT_LANGUAGE) {
                translation = translationsRepository.loadTranslation(movie, language, onlyLocal = true)
              }
              CalendarMovieListItem.MovieItem(
                movie = movie,
                image = imagesProvider.findCachedImage(movie, ImageType.POSTER),
                isWatched = myMovies.any { it.tmdbId == movie.tmdbId },
                isWatchlist = watchlistMovies.any { it.tmdbId == movie.tmdbId },
                dateFormat = dateFormat,
                translation = translation,
                spoilers = spoilers,
              )
            }
          }.awaitAll()

      val queryElements = filterByQuery(searchQuery ?: "", elements)
      val groupedItems = grouper.groupByTime(nowUtc(), queryElements)

      if (withFilters) {
        val filtersItem =
          when (this@CalendarMoviesItemsCase) {
            is CalendarMoviesFutureCase -> CalendarMovieListItem.Filters(PRESENT_FUTURE)
            is CalendarMoviesRecentsCase -> CalendarMovieListItem.Filters(RECENTS)
            else -> throw IllegalStateException()
          }
        listOf(filtersItem) + groupedItems
      } else {
        groupedItems
      }
    }

  private fun filterByQuery(
    query: String,
    items: List<CalendarMovieListItem.MovieItem>,
  ) = items.filter {
    it.movie.title
      .removeDiacritics()
      .contains(query, true) ||
      it.translation
        ?.title
        ?.removeDiacritics()
        ?.contains(query, true) == true ||
      it.movie.released
        ?.format(it.dateFormat)
        ?.contains(query, true) == true
  }
}
