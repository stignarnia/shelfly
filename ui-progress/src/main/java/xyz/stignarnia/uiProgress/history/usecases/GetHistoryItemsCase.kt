package xyz.stignarnia.uiProgress.history.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcZone
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.Episode
import xyz.stignarnia.dataLocal.database.model.Season
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.extensions.removeDiacritics
import xyz.stignarnia.uiModel.HistoryPeriod
import xyz.stignarnia.uiModel.HistoryPeriod.ALL_TIME
import xyz.stignarnia.uiModel.HistoryPeriod.LAST_30_DAYS
import xyz.stignarnia.uiModel.HistoryPeriod.LAST_365_DAYS
import xyz.stignarnia.uiModel.HistoryPeriod.LAST_90_DAYS
import xyz.stignarnia.uiModel.HistoryPeriod.LAST_MONTH
import xyz.stignarnia.uiModel.HistoryPeriod.LAST_WEEK
import xyz.stignarnia.uiModel.HistoryPeriod.THIS_MONTH
import xyz.stignarnia.uiModel.HistoryPeriod.THIS_WEEK
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiProgress.helpers.TranslationsBundle
import xyz.stignarnia.uiProgress.history.entities.HistoryListItem
import xyz.stignarnia.uiProgress.history.utilities.groupers.HistoryItemsGrouper
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters.firstDayOfMonth
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import xyz.stignarnia.uiModel.Episode as EpisodeUi

internal class GetHistoryItemsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val showsRepository: ShowsRepository,
    private val translationsRepository: TranslationsRepository,
    private val settingsRepository: SettingsRepository,
    private val imagesProvider: ShowImagesProvider,
    private val dateFormatProvider: DateFormatProvider,
    private val mappers: Mappers,
    private val grouper: HistoryItemsGrouper,
  ) {
    suspend fun loadItems(searchQuery: String? = "") =
      withContext(dispatchers.IO) {
        val shows =
          coroutineScope {
            val async1 = async { showsRepository.myShows.loadAll() }
            val async2 = async { showsRepository.watchlistShows.loadAll() }
            awaitAll(async1, async2).flatten()
          }
        val showsIds = shows.map { it.tmdbId }.chunked(250)

        val periodFilter = settingsRepository.filters.historyShowsPeriod
        val periodRange = getPeriodRange(periodFilter)

        // Awaited separately rather than through awaitAll, which erases two different element types to a common supertype and needs an unchecked cast to get them back.
        val episodesAsync =
          async {
            showsIds.fold(listOf<Episode>()) { acc, ids ->
              acc.plus(localSource.episodes.getAllWatchedForShows(ids, periodRange.first, periodRange.last))
            }
          }
        val seasonsAsync =
          async {
            showsIds.fold(listOf<Season>()) { acc, ids ->
              acc.plus(localSource.seasons.getAllByShowsIds(ids))
            }
          }
        val localEpisodes = episodesAsync.await()
        val localSeasons = seasonsAsync.await()

        val language = translationsRepository.getLanguage()
        val dateFormat = dateFormatProvider.loadFullHourFormat()

        val items =
          localEpisodes
            .map { episode ->
              async {
                val show = shows.firstOrNull { it.tmdbId == episode.idShowTmdb }
                val season =
                  localSeasons.firstOrNull {
                    it.idShowTmdb == episode.idShowTmdb &&
                      it.seasonNumber == episode.seasonNumber
                  }

                if (show == null || season == null) {
                  return@async null
                }

                val seasonEpisodes =
                  localEpisodes.filter {
                    it.idShowTmdb == season.idShowTmdb &&
                      it.seasonNumber == season.seasonNumber
                  }

                val episodeUi = mappers.episode.fromDatabase(episode)
                val seasonUi = mappers.season.fromDatabase(season, seasonEpisodes)

                HistoryListItem.Episode(
                  show = show,
                  season = seasonUi,
                  episode = episodeUi,
                  image = imagesProvider.findCachedImage(show, ImageType.POSTER),
                  translations = getTranslation(language, show, episodeUi),
                  dateFormat = dateFormat,
                )
              }
            }.awaitAll()
            .filterNotNull()

        val filtersItem = listOf(HistoryListItem.Filters(periodFilter))
        val searchItems = filterByQuery(searchQuery, dateFormat, items)
        val groupedItems =
          grouper.groupByDay(
            items = searchItems,
            language = language,
          )
        filtersItem + groupedItems
      }

    private fun filterByQuery(
      query: String?,
      dateFormat: DateTimeFormatter,
      items: List<HistoryListItem.Episode>,
    ): List<HistoryListItem.Episode> {
      if (query.isNullOrBlank()) {
        return items
      }
      return items.filter {
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
          it.episode.lastWatchedAt
            ?.toLocalZone()
            ?.format(dateFormat)
            ?.contains(query, true) == true
      }
    }

    private suspend fun getTranslation(
      language: String,
      show: Show,
      episode: EpisodeUi,
    ): TranslationsBundle? {
      if (language == DEFAULT_LANGUAGE) {
        return null
      }
      return TranslationsBundle(
        episode =
          translationsRepository.loadTranslation(
            language = language,
            showId = show.ids.tmdb,
            episode = episode,
            onlyLocal = true,
          ),
        show =
          translationsRepository.loadTranslation(
            language = language,
            show = show,
            onlyLocal = true,
          ),
      )
    }

    private fun getPeriodRange(period: HistoryPeriod): LongRange {
      val nowUtcMillis = nowUtcMillis()
      return when (period) {
        THIS_WEEK -> {
          val nowLocal = dateFromMillis(nowUtcMillis).toLocalZone()

          val weekStartLocal =
            nowLocal
              .minusDays(nowLocal.dayOfWeek.ordinal.toLong())
              .with(LocalTime.MIN)
          val weekStartUtc = weekStartLocal.toUtcZone().toMillis()

          val weekEndLocal = weekStartLocal.plusDays(6).with(LocalTime.MAX)
          val weekEndUtc = weekEndLocal.toUtcZone().toMillis()

          return weekStartUtc..weekEndUtc
        }

        LAST_WEEK -> {
          val now = dateFromMillis(nowUtcMillis).toLocalZone()

          val weekStartLocal =
            now
              .minusDays(now.dayOfWeek.ordinal.toLong())
              .minusWeeks(1)
              .with(LocalTime.MIN)
          val weekStartUtc = weekStartLocal.toUtcZone().toMillis()

          val weekEndLocal = weekStartLocal.plusDays(6).with(LocalTime.MAX)
          val weekEndUtc = weekEndLocal.toUtcZone().toMillis()

          return weekStartUtc..weekEndUtc
        }

        THIS_MONTH -> {
          val now = dateFromMillis(nowUtcMillis).toLocalZone()

          val monthStartLocal = now.with(firstDayOfMonth()).with(LocalTime.MIN)
          val monthStartUtc = monthStartLocal.toUtcZone().toMillis()

          val monthEndLocal = monthStartLocal.with(lastDayOfMonth()).with(LocalTime.MAX)
          val monthEndUtc = monthEndLocal.toUtcZone().toMillis()

          return monthStartUtc..monthEndUtc
        }

        LAST_MONTH -> {
          val now = dateFromMillis(nowUtcMillis).toLocalZone()

          val monthStartLocal =
            now
              .with(firstDayOfMonth())
              .minusDays(1)
              .with(firstDayOfMonth())
              .with(LocalTime.MIN)
          val monthStartUtc = monthStartLocal.toUtcZone().toMillis()

          val monthEndLocal = monthStartLocal.with(lastDayOfMonth()).with(LocalTime.MAX)
          val monthEndUtc = monthEndLocal.toUtcZone().toMillis()

          return monthStartUtc..monthEndUtc
        }

        LAST_30_DAYS -> {
          (nowUtcMillis - 30.days.inWholeMilliseconds)..nowUtcMillis
        }

        LAST_90_DAYS -> {
          (nowUtcMillis - 90.days.inWholeMilliseconds)..nowUtcMillis
        }

        LAST_365_DAYS -> {
          (nowUtcMillis - 365.days.inWholeMilliseconds)..nowUtcMillis
        }

        ALL_TIME -> {
          (nowUtcMillis - 36159.days.inWholeMilliseconds)..nowUtcMillis
        } // Limited to 99 years
      }
    }
  }
