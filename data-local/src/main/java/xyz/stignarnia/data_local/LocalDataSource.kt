package xyz.stignarnia.data_local

import xyz.stignarnia.data_local.sources.ArchiveMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.ArchiveShowsLocalDataSource
import xyz.stignarnia.data_local.sources.CustomListsItemsLocalDataSource
import xyz.stignarnia.data_local.sources.CustomListsLocalDataSource
import xyz.stignarnia.data_local.sources.DiscoverMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.DiscoverShowsLocalDataSource
import xyz.stignarnia.data_local.sources.EpisodeTranslationsLocalDataSource
import xyz.stignarnia.data_local.sources.EpisodesLocalDataSource
import xyz.stignarnia.data_local.sources.EpisodesSyncLogLocalDataSource
import xyz.stignarnia.data_local.sources.MovieImagesLocalDataSource
import xyz.stignarnia.data_local.sources.MovieRatingsLocalDataSource
import xyz.stignarnia.data_local.sources.MovieStreamingsLocalDataSource
import xyz.stignarnia.data_local.sources.MovieTranslationsLocalDataSource
import xyz.stignarnia.data_local.sources.MoviesLocalDataSource
import xyz.stignarnia.data_local.sources.MoviesSyncLogLocalDataSource
import xyz.stignarnia.data_local.sources.MyMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.MyShowsLocalDataSource
import xyz.stignarnia.data_local.sources.PeopleCreditsLocalDataSource
import xyz.stignarnia.data_local.sources.PeopleImagesLocalDataSource
import xyz.stignarnia.data_local.sources.PeopleLocalDataSource
import xyz.stignarnia.data_local.sources.PeopleShowsMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.RatingsLocalDataSource
import xyz.stignarnia.data_local.sources.RecentSearchLocalDataSource
import xyz.stignarnia.data_local.sources.RelatedMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.RelatedShowsLocalDataSource
import xyz.stignarnia.data_local.sources.SeasonsLocalDataSource
import xyz.stignarnia.data_local.sources.SettingsLocalDataSource
import xyz.stignarnia.data_local.sources.ShowImagesLocalDataSource
import xyz.stignarnia.data_local.sources.ShowRatingsLocalDataSource
import xyz.stignarnia.data_local.sources.ShowStreamingsLocalDataSource
import xyz.stignarnia.data_local.sources.ShowTranslationsLocalDataSource
import xyz.stignarnia.data_local.sources.ShowsLocalDataSource
import xyz.stignarnia.data_local.sources.TranslationsMoviesSyncLogLocalDataSource
import xyz.stignarnia.data_local.sources.TranslationsShowsSyncLogLocalDataSource
import xyz.stignarnia.data_local.sources.WatchlistMoviesLocalDataSource
import xyz.stignarnia.data_local.sources.WatchlistShowsLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

// TODO Refactor. Split or remove this wrapper at all. Clients do not need to be exposed to everything.

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
internal class MainLocalDataSource @Inject constructor(
  override val archiveMovies: ArchiveMoviesLocalDataSource,
  override val archiveShows: ArchiveShowsLocalDataSource,
  override val customLists: CustomListsLocalDataSource,
  override val customListsItems: CustomListsItemsLocalDataSource,
  override val discoverMovies: DiscoverMoviesLocalDataSource,
  override val discoverShows: DiscoverShowsLocalDataSource,
  override val episodes: EpisodesLocalDataSource,
  override val episodesSyncLog: EpisodesSyncLogLocalDataSource,
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
