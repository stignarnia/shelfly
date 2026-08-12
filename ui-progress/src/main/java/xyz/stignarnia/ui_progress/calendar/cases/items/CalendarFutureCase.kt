package xyz.stignarnia.ui_progress.calendar.cases.items

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Episode
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsFiltersRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_progress.calendar.helpers.WatchlistAppender
import xyz.stignarnia.ui_progress.calendar.helpers.filters.CalendarFutureFilter
import xyz.stignarnia.ui_progress.calendar.helpers.groupers.CalendarFutureGrouper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarFutureCase @Inject constructor(
  dispatchers: CoroutineDispatchers,
  localSource: LocalDataSource,
  mappers: Mappers,
  showsRepository: ShowsRepository,
  translationsRepository: TranslationsRepository,
  spoilersRepository: SettingsSpoilersRepository,
  filtersRepository: SettingsFiltersRepository,
  imagesProvider: ShowImagesProvider,
  dateFormatProvider: DateFormatProvider,
  watchlistAppender: WatchlistAppender,
  override val filter: CalendarFutureFilter,
  override val grouper: CalendarFutureGrouper,
) : CalendarItemsCase(
    dispatchers,
    localSource,
    mappers,
    showsRepository,
    translationsRepository,
    spoilersRepository,
    filtersRepository,
    imagesProvider,
    dateFormatProvider,
    watchlistAppender,
  ) {

  override fun sortEpisodes() =
    compareBy<Episode> { it.firstAired }
      .thenByDescending { it.idShowTmdb }
      .thenBy { it.episodeNumber }

  override fun isWatched(episode: Episode) = true

  override fun isSpoilerHidden(episode: Episode) = !episode.isWatched
}
