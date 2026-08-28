package xyz.stignarnia.repository.mappers

import xyz.stignarnia.uiModel.DiscoverFeed
import xyz.stignarnia.uiModel.DiscoverFeed.TRENDING
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.NotificationDelay
import xyz.stignarnia.uiModel.Settings
import javax.inject.Inject
import kotlin.enums.enumEntries
import xyz.stignarnia.dataLocal.database.model.Settings as SettingsDb

class SettingsMapper
  @Inject
  constructor() {
    fun fromDatabase(settings: SettingsDb) =
      Settings(
        isInitialRun = settings.isInitialRun,
        episodesNotificationsEnabled = settings.episodesNotificationsEnabled,
        episodesNotificationsDelay = NotificationDelay.fromDelay(settings.episodesNotificationsDelay),
        myShowsWatchingSortBy = enumValueOf(settings.myShowsRunningSortBy),
        myShowsUpcomingSortBy = enumValueOf(settings.myShowsIncomingSortBy),
        myShowsFinishedSortBy = enumValueOf(settings.myShowsEndedSortBy),
        myShowsAllSortBy = enumValueOf(settings.myShowsAllSortBy),
        myShowsRunningIsCollapsed = settings.myShowsRunningIsCollapsed,
        myShowsIncomingIsCollapsed = settings.myShowsIncomingIsCollapsed,
        myShowsEndedIsCollapsed = settings.myShowsEndedIsCollapsed,
        myRecentsAmount = settings.myShowsRecentsAmount,
        watchlistShowsSortBy = enumValueOf(settings.seeLaterShowsSortBy),
        archiveShowsSortBy = enumValueOf(settings.archiveShowsSortBy),
        showAnticipatedShows = settings.showAnticipatedShows,
        discoverFilterFeed =
          enumEntries<DiscoverFeed>()
            .find { settings.discoverFilterFeed == it.name } ?: TRENDING,
        discoverFilterGenres =
          settings.discoverFilterGenres
            .split(
              ",",
            ).filter { it.isNotBlank() }
            .map { Genre.valueOf(it) },
        progressSortOrder = enumValueOf(settings.watchlistSortBy),
        archiveIncludeStatistics = settings.archiveShowsIncludeStatistics,
        specialSeasonsEnabled = settings.specialSeasonsEnabled,
        showAnticipatedMovies = settings.showAnticipatedMovies,
        discoverMoviesFilterGenres =
          settings.discoverMoviesFilterGenres
            .split(",")
            .filter {
              it.isNotBlank()
            }.map { Genre.valueOf(it) },
        discoverMoviesFilterFeed =
          enumEntries<DiscoverFeed>()
            .find { settings.discoverFilterFeed == it.name } ?: TRENDING,
        myMoviesAllSortBy = enumValueOf(settings.myMoviesAllSortBy),
        watchlistMoviesSortBy = enumValueOf(settings.seeLaterMoviesSortBy),
        progressMoviesSortBy = enumValueOf(settings.progressMoviesSortBy),
        showCollectionShows = settings.showCollectionShows,
        showCollectionMovies = settings.showCollectionMovies,
        widgetsShowLabel = settings.widgetsShowLabel,
        quickRateEnabled = settings.quickRateEnabled,
        listsSortBy = enumValueOf(settings.listsSortBy),
        progressUpcomingEnabled = settings.progressUpcomingEnabled,
      )

    fun toDatabase(settings: Settings) =
      SettingsDb(
        isInitialRun = settings.isInitialRun,
        pushNotificationsEnabled = false,
        episodesNotificationsEnabled = settings.episodesNotificationsEnabled,
        episodesNotificationsDelay = settings.episodesNotificationsDelay.delayMs,
        myShowsRunningSortBy = settings.myShowsWatchingSortBy.name,
        myShowsIncomingSortBy = settings.myShowsUpcomingSortBy.name,
        myShowsEndedSortBy = settings.myShowsFinishedSortBy.name,
        myShowsAllSortBy = settings.myShowsAllSortBy.name,
        myShowsRunningIsCollapsed = settings.myShowsRunningIsCollapsed,
        myShowsIncomingIsCollapsed = settings.myShowsIncomingIsCollapsed,
        myShowsEndedIsCollapsed = settings.myShowsEndedIsCollapsed,
        myShowsRunningIsEnabled = false,
        myShowsIncomingIsEnabled = false,
        myShowsEndedIsEnabled = false,
        myShowsRecentIsEnabled = false,
        myMoviesRecentIsEnabled = false,
        myShowsRecentsAmount = settings.myRecentsAmount,
        seeLaterShowsSortBy = settings.watchlistShowsSortBy.name,
        archiveShowsSortBy = settings.archiveShowsSortBy.name,
        showAnticipatedShows = settings.showAnticipatedShows,
        discoverFilterFeed = settings.discoverFilterFeed.name,
        discoverFilterGenres = settings.discoverFilterGenres.joinToString(",") { it.name },
        // Held the old channel-name filter, which never reached TMDB.
        // The streaming filter that replaced it is region-scoped, so it lives with the region in preferences rather than in the synced settings row.
        discoverFilterNetworks = "",
        watchlistSortBy = settings.progressSortOrder.name,
        archiveShowsIncludeStatistics = settings.archiveIncludeStatistics,
        specialSeasonsEnabled = settings.specialSeasonsEnabled,
        showAnticipatedMovies = settings.showAnticipatedMovies,
        discoverMoviesFilterFeed = settings.discoverMoviesFilterFeed.name,
        discoverMoviesFilterGenres = settings.discoverMoviesFilterGenres.joinToString(",") { it.name },
        myMoviesAllSortBy = settings.myMoviesAllSortBy.name,
        seeLaterMoviesSortBy = settings.watchlistMoviesSortBy.name,
        progressMoviesSortBy = settings.progressMoviesSortBy.name,
        showCollectionShows = settings.showCollectionShows,
        showCollectionMovies = settings.showCollectionMovies,
        widgetsShowLabel = settings.widgetsShowLabel,
        quickRateEnabled = settings.quickRateEnabled,
        listsSortBy = settings.listsSortBy.name,
        progressUpcomingEnabled = settings.progressUpcomingEnabled,
      )
  }
