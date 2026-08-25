package xyz.stignarnia.ui_progress.calendar.cases.items

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Episode
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsFiltersRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_base.utilities.extensions.removeDiacritics
import xyz.stignarnia.ui_model.CalendarMode.PRESENT_FUTURE
import xyz.stignarnia.ui_model.CalendarMode.RECENTS
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_progress.calendar.helpers.WatchlistAppender
import xyz.stignarnia.ui_progress.calendar.helpers.filters.CalendarFilter
import xyz.stignarnia.ui_progress.calendar.helpers.groupers.CalendarGrouper
import xyz.stignarnia.ui_progress.calendar.recycler.CalendarListItem
import xyz.stignarnia.ui_progress.helpers.TranslationsBundle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

abstract class CalendarItemsCase(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
  private val showsRepository: ShowsRepository,
  private val translationsRepository: TranslationsRepository,
  private val spoilersRepository: SettingsSpoilersRepository,
  private val filtersRepository: SettingsFiltersRepository,
  private val imagesProvider: ShowImagesProvider,
  private val dateFormatProvider: DateFormatProvider,
  private val watchlistAppender: WatchlistAppender,
) {

  abstract val filter: CalendarFilter
  abstract val grouper: CalendarGrouper

  abstract fun sortEpisodes(): Comparator<Episode>

  abstract fun isWatched(episode: Episode): Boolean

  abstract fun isSpoilerHidden(episode: Episode): Boolean

  suspend fun loadItems(
    searchQuery: String? = "",
    withFilters: Boolean = true,
  ): List<CalendarListItem> {
    return withContext(dispatchers.IO) {
      val now = nowUtc().toLocalZone()

      val language = translationsRepository.getLanguage()
      val dateFormat = dateFormatProvider.loadFullHourFormat()
      val spoilers = spoilersRepository.getAll()
      val premieresOnly = filtersRepository.calendarPremieresOnly

      val (myShows, watchlistShows) = coroutineScope {
        val async1 = async { showsRepository.myShows.loadAll() }
        val async2 = async { showsRepository.watchlistShows.loadAll() }
        awaitAll(async1, async2)
      }

      val shows = myShows + watchlistShows

      val showsIds = shows.map { it.tmdbId }.chunked(250)
      val watchlistShowsIds = watchlistShows.map { it.tmdbId }

      // Awaited separately rather than through awaitAll, which erases two different element types to a common supertype and needs an unchecked cast to get them back.
      val episodesAsync = async {
        showsIds.fold(mutableListOf<Episode>()) { acc, list ->
          acc += localSource.episodes.getAllByShowsIds(list)
          acc
        }
      }
      val seasonsAsync = async {
        showsIds.fold(mutableListOf<Season>()) { acc, list ->
          acc += localSource.seasons.getAllByShowsIds(list)
          acc
        }
      }
      val episodes = episodesAsync.await()
      val seasons = seasonsAsync.await()

      val filteredSeasons = seasons.filter { it.seasonNumber != 0 }.toMutableList()
      val filteredEpisodes = episodes.filter { it.seasonNumber != 0 }.toMutableList()

      watchlistAppender.appendWatchlistShows(
        watchlistShows,
        filteredSeasons,
        filteredEpisodes,
      )

      val elements = filteredEpisodes
        .filter { filter.filter(now, it, premieresOnly) }
        .sortedWith(sortEpisodes())
        .map { episode ->
          async {
            val show = shows.firstOrNull { it.tmdbId == episode.idShowTmdb }
            val season = filteredSeasons.firstOrNull {
              it.idShowTmdb == episode.idShowTmdb && it.seasonNumber == episode.seasonNumber
            }

            if (show == null || season == null) {
              return@async null
            }

            val seasonEpisodes = episodes.filter {
              it.idShowTmdb == season.idShowTmdb &&
                it.seasonNumber == season.seasonNumber
            }

            val episodeUi = mappers.episode.fromDatabase(episode)
            val seasonUi = mappers.season.fromDatabase(season, seasonEpisodes)

            var translations: TranslationsBundle? = null
            if (language != Config.DEFAULT_LANGUAGE) {
              translations = TranslationsBundle(
                episode = translationsRepository.loadTranslation(episodeUi, show.ids.tmdb, language, onlyLocal = true),
                show = translationsRepository.loadTranslation(show, language, onlyLocal = true),
              )
            }
            CalendarListItem.Episode(
              show = show,
              image = imagesProvider.findCachedImage(show, ImageType.POSTER),
              episode = episodeUi,
              season = seasonUi,
              isWatched = isWatched(episode),
              isWatchlist = show.tmdbId in watchlistShowsIds,
              isSpoilerHidden = isSpoilerHidden(episode),
              dateFormat = dateFormat,
              translations = translations,
              spoilers = spoilers,
            )
          }
        }.awaitAll()
        .filterNotNull()

      val queryElements = filterByQuery(searchQuery ?: "", elements)
      val groupedItems = grouper.groupByTime(queryElements)

      if (withFilters) {
        val filtersItem = when (this@CalendarItemsCase) {
          is CalendarFutureCase -> CalendarListItem.Filters(PRESENT_FUTURE, premieresOnly)
          is CalendarRecentsCase -> CalendarListItem.Filters(RECENTS, premieresOnly)
          else -> throw IllegalStateException()
        }
        listOf(filtersItem) + groupedItems
      } else {
        groupedItems
      }
    }
  }

  private fun filterByQuery(
    query: String,
    items: List<CalendarListItem.Episode>,
  ) = items.filter {
    it.show.title
      .removeDiacritics()
      .contains(query, true) ||
      it.episode.title
        .removeDiacritics()
        .contains(query, true) ||
      it.translations
        ?.show
        ?.title
        ?.removeDiacritics()
        ?.contains(query, true) == true ||
      it.translations
        ?.episode
        ?.title
        ?.removeDiacritics()
        ?.contains(query, true) == true ||
      it.episode.firstAired
        ?.toLocalZone()
        ?.format(it.dateFormat)
        ?.contains(query, true) == true
  }
}
