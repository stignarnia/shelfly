package xyz.stignarnia.data_local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import xyz.stignarnia.data_local.database.dao.ArchiveMoviesDao
import xyz.stignarnia.data_local.database.dao.ArchiveShowsDao
import xyz.stignarnia.data_local.database.dao.CustomListsDao
import xyz.stignarnia.data_local.database.dao.CustomListsItemsDao
import xyz.stignarnia.data_local.database.dao.DiscoverMoviesDao
import xyz.stignarnia.data_local.database.dao.DiscoverShowsDao
import xyz.stignarnia.data_local.database.dao.EpisodeTranslationsDao
import xyz.stignarnia.data_local.database.dao.EpisodesDao
import xyz.stignarnia.data_local.database.dao.EpisodesSyncLogDao
import xyz.stignarnia.data_local.database.dao.MovieCollectionsDao
import xyz.stignarnia.data_local.database.dao.MovieCollectionsItemsDao
import xyz.stignarnia.data_local.database.dao.MovieImagesDao
import xyz.stignarnia.data_local.database.dao.MovieRatingsDao
import xyz.stignarnia.data_local.database.dao.MovieStreamingsDao
import xyz.stignarnia.data_local.database.dao.MovieTranslationsDao
import xyz.stignarnia.data_local.database.dao.MoviesDao
import xyz.stignarnia.data_local.database.dao.MoviesSyncLogDao
import xyz.stignarnia.data_local.database.dao.MyMoviesDao
import xyz.stignarnia.data_local.database.dao.MyShowsDao
import xyz.stignarnia.data_local.database.dao.PeopleCreditsDao
import xyz.stignarnia.data_local.database.dao.PeopleDao
import xyz.stignarnia.data_local.database.dao.PeopleImagesDao
import xyz.stignarnia.data_local.database.dao.PeopleShowsMoviesDao
import xyz.stignarnia.data_local.database.dao.RatingsDao
import xyz.stignarnia.data_local.database.dao.RecentSearchDao
import xyz.stignarnia.data_local.database.dao.RelatedMoviesDao
import xyz.stignarnia.data_local.database.dao.RelatedShowsDao
import xyz.stignarnia.data_local.database.dao.SeasonsDao
import xyz.stignarnia.data_local.database.dao.SettingsDao
import xyz.stignarnia.data_local.database.dao.ShowImagesDao
import xyz.stignarnia.data_local.database.dao.ShowRatingsDao
import xyz.stignarnia.data_local.database.dao.ShowStreamingsDao
import xyz.stignarnia.data_local.database.dao.ShowTranslationsDao
import xyz.stignarnia.data_local.database.dao.ShowsDao
import xyz.stignarnia.data_local.database.dao.TranslationsMoviesSyncLogDao
import xyz.stignarnia.data_local.database.dao.TranslationsSyncLogDao
import xyz.stignarnia.data_local.database.dao.WatchlistMoviesDao
import xyz.stignarnia.data_local.database.dao.WatchlistShowsDao
import xyz.stignarnia.data_local.database.migrations.DATABASE_VERSION
import xyz.stignarnia.data_local.database.model.ArchiveMovie
import xyz.stignarnia.data_local.database.model.ArchiveShow
import xyz.stignarnia.data_local.database.model.CustomList
import xyz.stignarnia.data_local.database.model.CustomListItem
import xyz.stignarnia.data_local.database.model.DiscoverMovie
import xyz.stignarnia.data_local.database.model.DiscoverShow
import xyz.stignarnia.data_local.database.model.Episode
import xyz.stignarnia.data_local.database.model.EpisodeTranslation
import xyz.stignarnia.data_local.database.model.EpisodesSyncLog
import xyz.stignarnia.data_local.database.model.Movie
import xyz.stignarnia.data_local.database.model.MovieCollection
import xyz.stignarnia.data_local.database.model.MovieCollectionItem
import xyz.stignarnia.data_local.database.model.MovieImage
import xyz.stignarnia.data_local.database.model.MovieRatings
import xyz.stignarnia.data_local.database.model.MovieStreaming
import xyz.stignarnia.data_local.database.model.MovieTranslation
import xyz.stignarnia.data_local.database.model.MoviesSyncLog
import xyz.stignarnia.data_local.database.model.MyMovie
import xyz.stignarnia.data_local.database.model.MyShow
import xyz.stignarnia.data_local.database.model.Person
import xyz.stignarnia.data_local.database.model.PersonCredits
import xyz.stignarnia.data_local.database.model.PersonImage
import xyz.stignarnia.data_local.database.model.PersonShowMovie
import xyz.stignarnia.data_local.database.model.Rating
import xyz.stignarnia.data_local.database.model.RecentSearch
import xyz.stignarnia.data_local.database.model.RelatedMovie
import xyz.stignarnia.data_local.database.model.RelatedShow
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.data_local.database.model.Settings
import xyz.stignarnia.data_local.database.model.Show
import xyz.stignarnia.data_local.database.model.ShowImage
import xyz.stignarnia.data_local.database.model.ShowRatings
import xyz.stignarnia.data_local.database.model.ShowStreaming
import xyz.stignarnia.data_local.database.model.ShowTranslation
import xyz.stignarnia.data_local.database.model.TranslationsMoviesSyncLog
import xyz.stignarnia.data_local.database.model.TranslationsSyncLog
import xyz.stignarnia.data_local.database.model.WatchlistMovie
import xyz.stignarnia.data_local.database.model.WatchlistShow

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
    Rating::class,
    ShowRatings::class,
    MovieRatings::class,
    ShowStreaming::class,
    MovieStreaming::class,
    MovieCollection::class,
    MovieCollectionItem::class,
  ],
  exportSchema = false,
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
}
