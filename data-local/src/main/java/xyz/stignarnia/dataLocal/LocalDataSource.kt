package xyz.stignarnia.dataLocal

import xyz.stignarnia.dataLocal.sources.ArchiveMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.ArchiveShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.CustomListsItemsLocalDataSource
import xyz.stignarnia.dataLocal.sources.CustomListsLocalDataSource
import xyz.stignarnia.dataLocal.sources.DiscoverMoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.DiscoverShowsLocalDataSource
import xyz.stignarnia.dataLocal.sources.EpisodeTranslationsLocalDataSource
import xyz.stignarnia.dataLocal.sources.EpisodesLocalDataSource
import xyz.stignarnia.dataLocal.sources.EpisodesSyncLogLocalDataSource
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
import javax.inject.Inject
import javax.inject.Singleton

// TODO Refactor.
// Split or remove this wrapper at all.
// Clients do not need to be exposed to everything.

/**
 * Provides local data sources access points.
 */
interface LocalDataSource {
  val archiveMovies: ArchiveMoviesLocalDataSource
  val archiveShows: ArchiveShowsLocalDataSource
  val customLists: CustomListsLocalDataSource
  val customListsItems: CustomListsItemsLocalDataSource
  val discoverMovies: DiscoverMoviesLocalDataSource
  val discoverShows: DiscoverShowsLocalDataSource
  val episodes: EpisodesLocalDataSource
  val episodesSyncLog: EpisodesSyncLogLocalDataSource
  val syncTombstones: SyncTombstonesLocalDataSource
  val episodesTranslations: EpisodeTranslationsLocalDataSource
  val movieImages: MovieImagesLocalDataSource
  val movieRatings: MovieRatingsLocalDataSource
  val movieStreamings: MovieStreamingsLocalDataSource
  val movieTranslations: MovieTranslationsLocalDataSource
  val movies: MoviesLocalDataSource
  val moviesSyncLog: MoviesSyncLogLocalDataSource
  val myMovies: MyMoviesLocalDataSource
  val myShows: MyShowsLocalDataSource
  val people: PeopleLocalDataSource
  val peopleCredits: PeopleCreditsLocalDataSource
  val peopleImages: PeopleImagesLocalDataSource
  val peopleShowsMovies: PeopleShowsMoviesLocalDataSource
  val ratings: RatingsLocalDataSource
  val recentSearch: RecentSearchLocalDataSource
  val relatedMovies: RelatedMoviesLocalDataSource
  val relatedShows: RelatedShowsLocalDataSource
  val seasons: SeasonsLocalDataSource
  val settings: SettingsLocalDataSource
  val showImages: ShowImagesLocalDataSource
  val showRatings: ShowRatingsLocalDataSource
  val showStreamings: ShowStreamingsLocalDataSource
  val showTranslations: ShowTranslationsLocalDataSource
  val shows: ShowsLocalDataSource
  val translationsMoviesSyncLog: TranslationsMoviesSyncLogLocalDataSource
  val translationsShowsSyncLog: TranslationsShowsSyncLogLocalDataSource
  val watchlistMovies: WatchlistMoviesLocalDataSource
  val watchlistShows: WatchlistShowsLocalDataSource
}

@Singleton
internal class MainLocalDataSource
  @Inject
  constructor(
    override val archiveMovies: ArchiveMoviesLocalDataSource,
    override val archiveShows: ArchiveShowsLocalDataSource,
    override val customLists: CustomListsLocalDataSource,
    override val customListsItems: CustomListsItemsLocalDataSource,
    override val discoverMovies: DiscoverMoviesLocalDataSource,
    override val discoverShows: DiscoverShowsLocalDataSource,
    override val episodes: EpisodesLocalDataSource,
    override val episodesSyncLog: EpisodesSyncLogLocalDataSource,
    override val syncTombstones: SyncTombstonesLocalDataSource,
    override val episodesTranslations: EpisodeTranslationsLocalDataSource,
    override val movieImages: MovieImagesLocalDataSource,
    override val movieRatings: MovieRatingsLocalDataSource,
    override val movieStreamings: MovieStreamingsLocalDataSource,
    override val movieTranslations: MovieTranslationsLocalDataSource,
    override val movies: MoviesLocalDataSource,
    override val moviesSyncLog: MoviesSyncLogLocalDataSource,
    override val myMovies: MyMoviesLocalDataSource,
    override val myShows: MyShowsLocalDataSource,
    override val people: PeopleLocalDataSource,
    override val peopleCredits: PeopleCreditsLocalDataSource,
    override val peopleImages: PeopleImagesLocalDataSource,
    override val peopleShowsMovies: PeopleShowsMoviesLocalDataSource,
    override val ratings: RatingsLocalDataSource,
    override val recentSearch: RecentSearchLocalDataSource,
    override val relatedMovies: RelatedMoviesLocalDataSource,
    override val relatedShows: RelatedShowsLocalDataSource,
    override val seasons: SeasonsLocalDataSource,
    override val settings: SettingsLocalDataSource,
    override val showImages: ShowImagesLocalDataSource,
    override val showRatings: ShowRatingsLocalDataSource,
    override val showStreamings: ShowStreamingsLocalDataSource,
    override val showTranslations: ShowTranslationsLocalDataSource,
    override val shows: ShowsLocalDataSource,
    override val translationsMoviesSyncLog: TranslationsMoviesSyncLogLocalDataSource,
    override val translationsShowsSyncLog: TranslationsShowsSyncLogLocalDataSource,
    override val watchlistMovies: WatchlistMoviesLocalDataSource,
    override val watchlistShows: WatchlistShowsLocalDataSource,
  ) : LocalDataSource
