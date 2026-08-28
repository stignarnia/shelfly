package xyz.stignarnia.dataRemote.tmdb

import xyz.stignarnia.dataRemote.catalog.model.Episode
import xyz.stignarnia.dataRemote.catalog.model.Movie
import xyz.stignarnia.dataRemote.catalog.model.MovieCollection
import xyz.stignarnia.dataRemote.catalog.model.PersonCredit
import xyz.stignarnia.dataRemote.catalog.model.SearchResult
import xyz.stignarnia.dataRemote.catalog.model.Season
import xyz.stignarnia.dataRemote.catalog.model.SeasonTranslation
import xyz.stignarnia.dataRemote.catalog.model.Show
import xyz.stignarnia.dataRemote.catalog.model.Translation
import xyz.stignarnia.dataRemote.tmdb.model.TmdbImage
import xyz.stignarnia.dataRemote.tmdb.model.TmdbImages
import xyz.stignarnia.dataRemote.tmdb.model.TmdbPerson
import xyz.stignarnia.dataRemote.tmdb.model.TmdbStreamingCountry
import xyz.stignarnia.dataRemote.tmdb.model.TmdbTranslation
import xyz.stignarnia.dataRemote.tmdb.model.TmdbWatchProvider

/**
 * Fetch/post remote resources via TMDB API
 */
interface TmdbRemoteDataSource {
  suspend fun fetchShowImages(tmdbId: Long): TmdbImages

  suspend fun fetchEpisodeImage(
    showTmdbId: Long?,
    season: Int?,
    episode: Int?,
  ): TmdbImage?

  suspend fun fetchMovieImages(tmdbId: Long): TmdbImages

  suspend fun fetchMoviePeople(tmdbId: Long): Map<TmdbPerson.Type, List<TmdbPerson>>

  suspend fun fetchShowPeople(tmdbId: Long): Map<TmdbPerson.Type, List<TmdbPerson>>

  suspend fun fetchShowWatchProviders(
    tmdbId: Long,
    countryCode: String,
  ): TmdbStreamingCountry?

  suspend fun fetchMovieWatchProviders(
    tmdbId: Long,
    countryCode: String,
  ): TmdbStreamingCountry?

  /**
   * Every provider carrying content in [countryCode], ordered the way TMDB ranks them for that region rather than globally.
   */
  suspend fun fetchWatchProviders(
    isMovie: Boolean,
    countryCode: String,
  ): List<TmdbWatchProvider>

  suspend fun fetchPersonDetails(id: Long): TmdbPerson

  suspend fun fetchPersonTranslations(id: Long): Map<String, TmdbTranslation.Data>

  suspend fun fetchPersonImages(tmdbId: Long): TmdbImages

  // Catalog access.
  // These return the shared data-remote DTOs so the repository layer is unaffected by the source swap.

  suspend fun fetchShow(
    tmdbId: Long,
    language: String? = null,
  ): Show

  suspend fun fetchMovie(
    tmdbId: Long,
    language: String? = null,
  ): Movie

  suspend fun fetchSeasons(tmdbId: Long): List<Season>

  suspend fun fetchTrendingShows(
    genres: List<String>,
    providers: List<Long> = emptyList(),
    countryCode: String = "",
    limit: Int,
  ): List<Show>

  suspend fun fetchTrendingMovies(
    genres: List<String>,
    providers: List<Long> = emptyList(),
    countryCode: String = "",
    limit: Int,
  ): List<Movie>

  suspend fun fetchPopularShows(
    genres: List<String>,
    providers: List<Long> = emptyList(),
    countryCode: String = "",
    limit: Int,
  ): List<Show>

  suspend fun fetchPopularMovies(
    genres: List<String>,
    providers: List<Long> = emptyList(),
    countryCode: String = "",
    limit: Int,
  ): List<Movie>

  suspend fun fetchAnticipatedShows(
    genres: List<String>,
    providers: List<Long> = emptyList(),
    countryCode: String = "",
    limit: Int,
  ): List<Show>

  suspend fun fetchAnticipatedMovies(
    genres: List<String>,
    providers: List<Long> = emptyList(),
    countryCode: String = "",
    limit: Int,
  ): List<Movie>

  suspend fun fetchNextEpisode(tmdbId: Long): Episode?

  suspend fun fetchPersonCredits(
    tmdbId: Long,
    type: TmdbPerson.Type,
  ): List<PersonCredit>

  suspend fun fetchShowTranslation(
    tmdbId: Long,
    language: String,
  ): Translation?

  suspend fun fetchMovieTranslation(
    tmdbId: Long,
    language: String,
  ): Translation?

  suspend fun fetchSeasonTranslations(
    tmdbId: Long,
    seasonNumber: Int,
    language: String,
  ): List<SeasonTranslation>

  suspend fun fetchMovieCollections(tmdbId: Long): List<MovieCollection>

  suspend fun fetchMovieCollectionItems(collectionId: Long): List<Movie>

  suspend fun fetchRelatedShows(tmdbId: Long): List<Show>

  suspend fun fetchRelatedMovies(tmdbId: Long): List<Movie>

  suspend fun fetchSearchResults(query: String): List<SearchResult>

  suspend fun fetchShowByImdbId(imdbId: String): Show?

  suspend fun fetchMovieByImdbId(imdbId: String): Movie?
}
