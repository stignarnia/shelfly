package xyz.stignarnia.ui_progress_movies.calendar.cases.items

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_progress_movies.calendar.helpers.filters.CalendarFutureFilter
import xyz.stignarnia.ui_progress_movies.calendar.helpers.groupers.CalendarFutureGrouper
import xyz.stignarnia.ui_progress_movies.calendar.helpers.sorter.CalendarFutureSorter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarMoviesFutureCase @Inject constructor(
  dispatchers: CoroutineDispatchers,
  moviesRepository: MoviesRepository,
  translationsRepository: TranslationsRepository,
  settingsSpoilersRepository: SettingsSpoilersRepository,
  imagesProvider: MovieImagesProvider,
  dateFormatProvider: DateFormatProvider,
  override val filter: CalendarFutureFilter,
  override val grouper: CalendarFutureGrouper,
  override val sorter: CalendarFutureSorter,
) : CalendarMoviesItemsCase(
    dispatchers,
    moviesRepository,
    translationsRepository,
    settingsSpoilersRepository,
    imagesProvider,
    dateFormatProvider,
  )
