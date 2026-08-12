package xyz.stignarnia.ui_progress_movies.calendar.cases.items

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_progress_movies.calendar.helpers.filters.CalendarRecentsFilter
import xyz.stignarnia.ui_progress_movies.calendar.helpers.groupers.CalendarRecentsGrouper
import xyz.stignarnia.ui_progress_movies.calendar.helpers.sorter.CalendarRecentsSorter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarMoviesRecentsCase @Inject constructor(
  dispatchers: CoroutineDispatchers,
  moviesRepository: MoviesRepository,
  translationsRepository: TranslationsRepository,
  settingsSpoilersRepository: SettingsSpoilersRepository,
  imagesProvider: MovieImagesProvider,
  dateFormatProvider: DateFormatProvider,
  override val filter: CalendarRecentsFilter,
  override val grouper: CalendarRecentsGrouper,
  override val sorter: CalendarRecentsSorter,
) : CalendarMoviesItemsCase(
    dispatchers,
    moviesRepository,
    translationsRepository,
    settingsSpoilersRepository,
    imagesProvider,
    dateFormatProvider,
  )
