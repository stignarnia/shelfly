package xyz.stignarnia.dataLocal.database

import androidx.room.Database
import androidx.room.RoomDatabase
import xyz.stignarnia.dataLocal.database.dao.ArchiveMoviesDao
import xyz.stignarnia.dataLocal.database.dao.ArchiveShowsDao
import xyz.stignarnia.dataLocal.database.dao.CustomListsDao
import xyz.stignarnia.dataLocal.database.dao.CustomListsItemsDao
import xyz.stignarnia.dataLocal.database.dao.DiscoverMoviesDao
import xyz.stignarnia.dataLocal.database.dao.DiscoverShowsDao
import xyz.stignarnia.dataLocal.database.dao.EpisodeTranslationsDao
import xyz.stignarnia.dataLocal.database.dao.EpisodesDao
import xyz.stignarnia.dataLocal.database.dao.EpisodesSyncLogDao
import xyz.stignarnia.dataLocal.database.dao.MovieCollectionsDao
import xyz.stignarnia.dataLocal.database.dao.MovieCollectionsItemsDao
import xyz.stignarnia.dataLocal.database.dao.MovieImagesDao
import xyz.stignarnia.dataLocal.database.dao.MovieRatingsDao
import xyz.stignarnia.dataLocal.database.dao.MovieStreamingsDao
import xyz.stignarnia.dataLocal.database.dao.MovieTranslationsDao
import xyz.stignarnia.dataLocal.database.dao.MoviesDao
import xyz.stignarnia.dataLocal.database.dao.MoviesSyncLogDao
import xyz.stignarnia.dataLocal.database.dao.MyMoviesDao
import xyz.stignarnia.dataLocal.database.dao.MyShowsDao
import xyz.stignarnia.dataLocal.database.dao.PeopleCreditsDao
import xyz.stignarnia.dataLocal.database.dao.PeopleDao
import xyz.stignarnia.dataLocal.database.dao.PeopleImagesDao
import xyz.stignarnia.dataLocal.database.dao.PeopleShowsMoviesDao
import xyz.stignarnia.dataLocal.database.dao.RatingsDao
import xyz.stignarnia.dataLocal.database.dao.RecentSearchDao
import xyz.stignarnia.dataLocal.database.dao.RelatedMoviesDao
import xyz.stignarnia.dataLocal.database.dao.RelatedShowsDao
import xyz.stignarnia.dataLocal.database.dao.SeasonsDao
import xyz.stignarnia.dataLocal.database.dao.SettingsDao
import xyz.stignarnia.dataLocal.database.dao.ShowImagesDao
import xyz.stignarnia.dataLocal.database.dao.ShowRatingsDao
import xyz.stignarnia.dataLocal.database.dao.ShowStreamingsDao
import xyz.stignarnia.dataLocal.database.dao.ShowTranslationsDao
import xyz.stignarnia.dataLocal.database.dao.ShowsDao
import xyz.stignarnia.dataLocal.database.dao.SyncTombstonesDao
import xyz.stignarnia.dataLocal.database.dao.TranslationsMoviesSyncLogDao
import xyz.stignarnia.dataLocal.database.dao.TranslationsSyncLogDao
import xyz.stignarnia.dataLocal.database.dao.WatchlistMoviesDao
import xyz.stignarnia.dataLocal.database.dao.WatchlistShowsDao
import xyz.stignarnia.dataLocal.database.migrations.DATABASE_VERSION
import xyz.stignarnia.dataLocal.database.model.ArchiveMovie
import xyz.stignarnia.dataLocal.database.model.ArchiveShow
import xyz.stignarnia.dataLocal.database.model.CustomList
import xyz.stignarnia.dataLocal.database.model.CustomListItem
import xyz.stignarnia.dataLocal.database.model.DiscoverMovie
import xyz.stignarnia.dataLocal.database.model.DiscoverShow
import xyz.stignarnia.dataLocal.database.model.Episode
import xyz.stignarnia.dataLocal.database.model.EpisodeTranslation
import xyz.stignarnia.dataLocal.database.model.EpisodesSyncLog
import xyz.stignarnia.dataLocal.database.model.Movie
import xyz.stignarnia.dataLocal.database.model.MovieCollection
import xyz.stignarnia.dataLocal.database.model.MovieCollectionItem
import xyz.stignarnia.dataLocal.database.model.MovieImage
import xyz.stignarnia.dataLocal.database.model.MovieRatings
import xyz.stignarnia.dataLocal.database.model.MovieStreaming
import xyz.stignarnia.dataLocal.database.model.MovieTranslation
import xyz.stignarnia.dataLocal.database.model.MoviesSyncLog
import xyz.stignarnia.dataLocal.database.model.MyMovie
import xyz.stignarnia.dataLocal.database.model.MyShow
import xyz.stignarnia.dataLocal.database.model.Person
import xyz.stignarnia.dataLocal.database.model.PersonCredits
import xyz.stignarnia.dataLocal.database.model.PersonImage
import xyz.stignarnia.dataLocal.database.model.PersonShowMovie
import xyz.stignarnia.dataLocal.database.model.Rating
import xyz.stignarnia.dataLocal.database.model.RecentSearch
import xyz.stignarnia.dataLocal.database.model.RelatedMovie
import xyz.stignarnia.dataLocal.database.model.RelatedShow
import xyz.stignarnia.dataLocal.database.model.Season
import xyz.stignarnia.dataLocal.database.model.Settings
import xyz.stignarnia.dataLocal.database.model.Show
import xyz.stignarnia.dataLocal.database.model.ShowImage
import xyz.stignarnia.dataLocal.database.model.ShowRatings
import xyz.stignarnia.dataLocal.database.model.ShowStreaming
import xyz.stignarnia.dataLocal.database.model.ShowTranslation
import xyz.stignarnia.dataLocal.database.model.SyncTombstone
import xyz.stignarnia.dataLocal.database.model.TranslationsMoviesSyncLog
import xyz.stignarnia.dataLocal.database.model.TranslationsSyncLog
import xyz.stignarnia.dataLocal.database.model.WatchlistMovie
import xyz.stignarnia.dataLocal.database.model.WatchlistShow

@Database(
  version = DATABASE_VERSION,
  entities = [
    Show::class,
    Movie::class,
    DiscoverShow::class,
    DiscoverMovie::class,
    MyShow::class,
    MyMovie::class,
    WatchlistShow::class,
    WatchlistMovie::class,
    ArchiveShow::class,
    ArchiveMovie::class,
    RelatedShow::class,
    RelatedMovie::class,
    ShowImage::class,
    MovieImage::class,
    Season::class,
    Person::class,
    PersonShowMovie::class,
    PersonCredits::class,
    PersonImage::class,
    Episode::class,
    Settings::class,
    RecentSearch::class,
    EpisodesSyncLog::class,
    MoviesSyncLog::class,
    TranslationsSyncLog::class,
    TranslationsMoviesSyncLog::class,
    ShowTranslation::class,
    MovieTranslation::class,
    EpisodeTranslation::class,
    CustomList::class,
    CustomListItem::class,
    SyncTombstone::class,
    Rating::class,
    ShowRatings::class,
    MovieRatings::class,
    ShowStreaming::class,
    MovieStreaming::class,
    MovieCollection::class,
    MovieCollectionItem::class,
  ],
  exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun showsDao(): ShowsDao

  abstract fun moviesDao(): MoviesDao

  abstract fun discoverShowsDao(): DiscoverShowsDao

  abstract fun discoverMoviesDao(): DiscoverMoviesDao

  abstract fun myShowsDao(): MyShowsDao

  abstract fun myMoviesDao(): MyMoviesDao

  abstract fun watchlistShowsDao(): WatchlistShowsDao

  abstract fun watchlistMoviesDao(): WatchlistMoviesDao

  abstract fun archiveShowsDao(): ArchiveShowsDao

  abstract fun archiveMoviesDao(): ArchiveMoviesDao

  abstract fun relatedShowsDao(): RelatedShowsDao

  abstract fun relatedMoviesDao(): RelatedMoviesDao

  abstract fun showImagesDao(): ShowImagesDao

  abstract fun movieImagesDao(): MovieImagesDao

  abstract fun recentSearchDao(): RecentSearchDao

  abstract fun episodesDao(): EpisodesDao

  abstract fun seasonsDao(): SeasonsDao

  abstract fun peopleDao(): PeopleDao

  abstract fun peopleShowsMoviesDao(): PeopleShowsMoviesDao

  abstract fun peopleCreditsDao(): PeopleCreditsDao

  abstract fun peopleImagesDao(): PeopleImagesDao

  abstract fun settingsDao(): SettingsDao

  abstract fun moviesSyncLogDao(): MoviesSyncLogDao

  abstract fun episodesSyncLogDao(): EpisodesSyncLogDao

  abstract fun translationsSyncLogDao(): TranslationsSyncLogDao

  abstract fun translationsMoviesSyncLogDao(): TranslationsMoviesSyncLogDao

  abstract fun showTranslationsDao(): ShowTranslationsDao

  abstract fun movieTranslationsDao(): MovieTranslationsDao

  abstract fun ratingsDao(): RatingsDao

  abstract fun showRatingsDao(): ShowRatingsDao

  abstract fun movieRatingsDao(): MovieRatingsDao

  abstract fun showStreamingsDao(): ShowStreamingsDao

  abstract fun movieStreamingsDao(): MovieStreamingsDao

  abstract fun movieCollectionsDao(): MovieCollectionsDao

  abstract fun movieCollectionsItemsDao(): MovieCollectionsItemsDao

  abstract fun episodeTranslationsDao(): EpisodeTranslationsDao

  abstract fun customListsDao(): CustomListsDao

  abstract fun customListsItemsDao(): CustomListsItemsDao

  abstract fun syncTombstonesDao(): SyncTombstonesDao
}
