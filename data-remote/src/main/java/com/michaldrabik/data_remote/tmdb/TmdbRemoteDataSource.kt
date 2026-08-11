package com.michaldrabik.data_remote.tmdb

import com.michaldrabik.data_remote.tmdb.model.TmdbImage
import com.michaldrabik.data_remote.tmdb.model.TmdbImages
import com.michaldrabik.data_remote.tmdb.model.TmdbPerson
import com.michaldrabik.data_remote.tmdb.model.TmdbStreamingCountry
import com.michaldrabik.data_remote.tmdb.model.TmdbTranslation
import com.michaldrabik.data_remote.trakt.model.Movie
import com.michaldrabik.data_remote.trakt.model.SearchResult
import com.michaldrabik.data_remote.trakt.model.Season
import com.michaldrabik.data_remote.trakt.model.Show

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

  suspend fun fetchPersonDetails(id: Long): TmdbPerson

  suspend fun fetchPersonTranslations(id: Long): Map<String, TmdbTranslation.Data>

  suspend fun fetchPersonImages(tmdbId: Long): TmdbImages

  // Catalog access, replacing the Trakt equivalents. These return the shared
  // data-remote DTOs so the repository layer is unaffected by the source swap.

  suspend fun fetchShow(
    tmdbId: Long,
    language: String? = null,
  ): Show

  suspend fun fetchMovie(
    tmdbId: Long,
    language: String? = null,
  ): Movie

  suspend fun fetchSeasons(tmdbId: Long): List<Season>

  suspend fun fetchTrendingShows(limit: Int): List<Show>

  suspend fun fetchTrendingMovies(limit: Int): List<Movie>

  suspend fun fetchPopularShows(limit: Int): List<Show>

  suspend fun fetchPopularMovies(limit: Int): List<Movie>

  suspend fun fetchAnticipatedShows(limit: Int): List<Show>

  suspend fun fetchAnticipatedMovies(limit: Int): List<Movie>

  suspend fun fetchRelatedShows(tmdbId: Long): List<Show>

  suspend fun fetchRelatedMovies(tmdbId: Long): List<Movie>

  suspend fun fetchSearchResults(query: String): List<SearchResult>

  suspend fun fetchShowByImdbId(imdbId: String): Show?

  suspend fun fetchMovieByImdbId(imdbId: String): Movie?
}
