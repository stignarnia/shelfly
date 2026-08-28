package xyz.stignarnia.uiProgress.calendar.cases.items

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.Episode
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsFiltersRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiProgress.calendar.helpers.WatchlistAppender
import xyz.stignarnia.uiProgress.calendar.helpers.filters.CalendarRecentsFilter
import xyz.stignarnia.uiProgress.calendar.helpers.groupers.CalendarRecentsGrouper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRecentsCase
  @Inject
  constructor(
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
    override val filter: CalendarRecentsFilter,
    override val grouper: CalendarRecentsGrouper,
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
      compareByDescending<Episode> { it.firstAired }
        .thenByDescending { it.idShowTmdb }
        .thenByDescending { it.episodeNumber }

    override fun isWatched(episode: Episode) = episode.isWatched

    override fun isSpoilerHidden(episode: Episode) = !episode.isWatched
  }
