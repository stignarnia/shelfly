package xyz.stignarnia.dataLocal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.dataLocal.database.AppDatabase
import xyz.stignarnia.dataLocal.sources.ArchiveMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.ArchiveShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.CustomListsItemsLocalDataSource
import xyz.stignarnia.dataLocal.sources.CustomListsLocalDataSource
import xyz.stignarnia.dataLocal.sources.DiscoverMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.DiscoverShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.EpisodeTranslationsLocalDataSource
import xyz.stignarnia.dataLocal.sources.EpisodesLocalDataSource
import xyz.stignarnia.dataLocal.sources.EpisodesSyncLogLocalDataSource
import xyz.stignarnia.dataLocal.sources.MovieCollectionsItemsLocalDataSource
import xyz.stignarnia.dataLocal.sources.MovieCollectionsLocalDataSource
import xyz.stignarnia.dataLocal.sources.MovieImagesLocalDataSource
import xyz.stignarnia.dataLocal.sources.MovieRatingsLocalDataSource
import xyz.stignarnia.dataLocal.sources.MovieStreamingsLocalDataSource
import xyz.stignarnia.dataLocal.sources.MovieTranslationsLocalDataSource
import xyz.stignarnia.dataLocal.sources.MoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.MoviesSyncLogLocalDataSource
import xyz.stignarnia.dataLocal.sources.MyMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.MyShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.PeopleCreditsLocalDataSource
import xyz.stignarnia.dataLocal.sources.PeopleImagesLocalDataSource
import xyz.stignarnia.dataLocal.sources.PeopleLocalDataSource
import xyz.stignarnia.dataLocal.sources.PeopleShowsMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.RatingsLocalDataSource
import xyz.stignarnia.dataLocal.sources.RecentSearchLocalDataSource
import xyz.stignarnia.dataLocal.sources.RelatedMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.RelatedShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.SeasonsLocalDataSource
import xyz.stignarnia.dataLocal.sources.SettingsLocalDataSource
import xyz.stignarnia.dataLocal.sources.ShowImagesLocalDataSource
import xyz.stignarnia.dataLocal.sources.ShowRatingsLocalDataSource
import xyz.stignarnia.dataLocal.sources.ShowStreamingsLocalDataSource
import xyz.stignarnia.dataLocal.sources.ShowTranslationsLocalDataSource
import xyz.stignarnia.dataLocal.sources.ShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.SyncTombstonesLocalDataSource
import xyz.stignarnia.dataLocal.sources.TranslationsMoviesSyncLogLocalDataSource
import xyz.stignarnia.dataLocal.sources.TranslationsShowsSyncLogLocalDataSource
import xyz.stignarnia.dataLocal.sources.WatchlistMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.WatchlistShowsLocalDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SourcesModule {
  @Provides
  @Singleton
  internal fun providesShows(database: AppDatabase): ShowsLocalDataSource = database.showsDao()

  @Provides
  @Singleton
  internal fun providesMovies(database: AppDatabase): MoviesLocalDataSource = database.moviesDao()

  @Provides
  @Singleton
  internal fun providesArchivesShows(database: AppDatabase): ArchiveShowsLocalDataSource = database.archiveShowsDao()

  @Provides
  @Singleton
  internal fun providesArchivesMovies(database: AppDatabase): ArchiveMoviesLocalDataSource = database.archiveMoviesDao()

  @Provides
  @Singleton
  internal fun providesCustomListsItems(database: AppDatabase): CustomListsItemsLocalDataSource =
    database.customListsItemsDao()

  @Provides
  @Singleton
  internal fun providesCustomLists(database: AppDatabase): CustomListsLocalDataSource = database.customListsDao()

  @Provides
  @Singleton
  internal fun providesDiscoverShows(database: AppDatabase): DiscoverShowsLocalDataSource = database.discoverShowsDao()

  @Provides
  @Singleton
  internal fun providesDiscoverMovies(database: AppDatabase): DiscoverMoviesLocalDataSource =
    database.discoverMoviesDao()

  @Provides
  @Singleton
  internal fun providesEpisodes(database: AppDatabase): EpisodesLocalDataSource = database.episodesDao()

  @Provides
  @Singleton
  internal fun providesEpisodesSyncLog(database: AppDatabase): EpisodesSyncLogLocalDataSource =
    database.episodesSyncLogDao()

  @Provides
  @Singleton
  internal fun providesSyncTombstones(database: AppDatabase): SyncTombstonesLocalDataSource =
    database.syncTombstonesDao()

  @Provides
  @Singleton
  internal fun providesEpisodesTranslations(database: AppDatabase): EpisodeTranslationsLocalDataSource =
    database.episodeTranslationsDao()

  @Provides
  @Singleton
  internal fun providesMovieImages(database: AppDatabase): MovieImagesLocalDataSource = database.movieImagesDao()

  @Provides
  @Singleton
  internal fun providesMovieRatings(database: AppDatabase): MovieRatingsLocalDataSource = database.movieRatingsDao()

  @Provides
  @Singleton
  internal fun providesMovieSyncLog(database: AppDatabase): MoviesSyncLogLocalDataSource = database.moviesSyncLogDao()

  @Provides
  @Singleton
  internal fun providesMovieStreaming(database: AppDatabase): MovieStreamingsLocalDataSource =
    database.movieStreamingsDao()

  @Provides
  @Singleton
  internal fun providesMovieCollections(database: AppDatabase): MovieCollectionsLocalDataSource =
    database.movieCollectionsDao()

  @Provides
  @Singleton
  internal fun providesMovieCollectionsItems(database: AppDatabase): MovieCollectionsItemsLocalDataSource =
    database.movieCollectionsItemsDao()

  @Provides
  @Singleton
  internal fun providesMovieTranslation(database: AppDatabase): MovieTranslationsLocalDataSource =
    database.movieTranslationsDao()

  @Provides
  @Singleton
  internal fun providesMyMovies(database: AppDatabase): MyMoviesLocalDataSource = database.myMoviesDao()

  @Provides
  @Singleton
  internal fun providesMyShows(database: AppDatabase): MyShowsLocalDataSource = database.myShowsDao()

  @Provides
  @Singleton
  internal fun providesPeopleCredits(database: AppDatabase): PeopleCreditsLocalDataSource = database.peopleCreditsDao()

  @Provides
  @Singleton
  internal fun providesPeople(database: AppDatabase): PeopleLocalDataSource = database.peopleDao()

  @Provides
  @Singleton
  internal fun providesPeopleImages(database: AppDatabase): PeopleImagesLocalDataSource = database.peopleImagesDao()

  @Provides
  @Singleton
  internal fun providesPeopleShowsMovies(database: AppDatabase): PeopleShowsMoviesLocalDataSource =
    database.peopleShowsMoviesDao()

  @Provides
  @Singleton
  internal fun providesRatings(database: AppDatabase): RatingsLocalDataSource = database.ratingsDao()

  @Provides
  @Singleton
  internal fun providesRecentSearch(database: AppDatabase): RecentSearchLocalDataSource = database.recentSearchDao()

  @Provides
  @Singleton
  internal fun providesSeasons(database: AppDatabase): SeasonsLocalDataSource = database.seasonsDao()

  @Provides
  @Singleton
  internal fun providesSettings(database: AppDatabase): SettingsLocalDataSource = database.settingsDao()

  @Provides
  @Singleton
  internal fun providesShowImages(database: AppDatabase): ShowImagesLocalDataSource = database.showImagesDao()

  @Provides
  @Singleton
  internal fun providesShowRatings(database: AppDatabase): ShowRatingsLocalDataSource = database.showRatingsDao()

  @Provides
  @Singleton
  internal fun providesShowStreamings(database: AppDatabase): ShowStreamingsLocalDataSource =
    database.showStreamingsDao()

  @Provides
  @Singleton
  internal fun providesShowTranslations(database: AppDatabase): ShowTranslationsLocalDataSource =
    database.showTranslationsDao()

  @Provides
  @Singleton
  internal fun providesTranslationsMovies(database: AppDatabase): TranslationsMoviesSyncLogLocalDataSource =
    database.translationsMoviesSyncLogDao()

  @Provides
  @Singleton
  internal fun providesTranslationsShows(database: AppDatabase): TranslationsShowsSyncLogLocalDataSource =
    database.translationsSyncLogDao()

  @Provides
  @Singleton
  internal fun providesWatchlistMovies(database: AppDatabase): WatchlistMoviesLocalDataSource =
    database.watchlistMoviesDao()

  @Provides
  @Singleton
  internal fun providesWatchlistShows(database: AppDatabase): WatchlistShowsLocalDataSource =
    database.watchlistShowsDao()

  @Provides
  @Singleton
  internal fun providesRelatedMovies(database: AppDatabase): RelatedMoviesLocalDataSource = database.relatedMoviesDao()

  @Provides
  @Singleton
  internal fun providesRelatedShows(database: AppDatabase): RelatedShowsLocalDataSource = database.relatedShowsDao()
}
