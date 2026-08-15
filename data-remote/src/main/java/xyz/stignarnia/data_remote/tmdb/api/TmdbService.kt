package xyz.stignarnia.data_remote.tmdb.api

import xyz.stignarnia.data_remote.tmdb.model.TmdbCollection
import xyz.stignarnia.data_remote.tmdb.model.TmdbFindResult
import xyz.stignarnia.data_remote.tmdb.model.TmdbImages
import xyz.stignarnia.data_remote.tmdb.model.TmdbMovie
import xyz.stignarnia.data_remote.tmdb.model.TmdbPage
import xyz.stignarnia.data_remote.tmdb.model.TmdbPeople
import xyz.stignarnia.data_remote.tmdb.model.TmdbPersonCredits
import xyz.stignarnia.data_remote.tmdb.model.TmdbPerson
import xyz.stignarnia.data_remote.tmdb.model.TmdbSearchItem
import xyz.stignarnia.data_remote.tmdb.model.TmdbSeason
import xyz.stignarnia.data_remote.tmdb.model.TmdbShow
import xyz.stignarnia.data_remote.tmdb.model.TmdbStreamings
import xyz.stignarnia.data_remote.tmdb.model.TmdbTranslationResponse
import xyz.stignarnia.data_remote.tmdb.model.TmdbWatchProviders
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {

  @GET("tv/{tmdbId}/images")
  suspend fun fetchShowImages(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbImages

  @GET("tv/{tmdbId}/season/{season}/episode/{episode}/images")
  suspend fun fetchEpisodeImages(
    @Path("tmdbId") tmdbId: Long?,
    @Path("season") seasonNumber: Int?,
    @Path("episode") episodeNumber: Int?,
  ): TmdbImages

  @GET("movie/{tmdbId}/images")
  suspend fun fetchMovieImages(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbImages

  @GET("person/{tmdbId}/images")
  suspend fun fetchPersonImages(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbImages

  @GET("person/{tmdbId}")
  suspend fun fetchPersonDetails(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbPerson

  @GET("person/{tmdbId}/translations")
  suspend fun fetchPersonTranslation(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbTranslationResponse

  @GET("movie/{tmdbId}/credits")
  suspend fun fetchMoviePeople(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbPeople

  @GET("tv/{tmdbId}/aggregate_credits")
  suspend fun fetchShowPeople(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbPeople

  @GET("movie/{tmdbId}/watch/providers")
  suspend fun fetchMovieWatchProviders(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbStreamings

  @GET("tv/{tmdbId}/watch/providers")
  suspend fun fetchShowWatchProviders(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbStreamings

  // The directory of every provider carrying content in a region, as opposed to
  // the providers carrying one title. Backs the discover availability filter.

  @GET("watch/providers/tv")
  suspend fun fetchShowProviders(
    @Query("watch_region") watchRegion: String,
  ): TmdbWatchProviders

  @GET("watch/providers/movie")
  suspend fun fetchMovieProviders(
    @Query("watch_region") watchRegion: String,
  ): TmdbWatchProviders

  // Catalog endpoints.

  @GET("tv/{tmdbId}?append_to_response=external_ids,content_ratings,videos")
  suspend fun fetchShow(
    @Path("tmdbId") tmdbId: Long,
    @Query("language") language: String? = null,
  ): TmdbShow

  @GET("movie/{tmdbId}?append_to_response=external_ids,release_dates,videos")
  suspend fun fetchMovie(
    @Path("tmdbId") tmdbId: Long,
    @Query("language") language: String? = null,
  ): TmdbMovie

  @GET("tv/{tmdbId}/season/{seasonNumber}")
  suspend fun fetchSeason(
    @Path("tmdbId") tmdbId: Long,
    @Path("seasonNumber") seasonNumber: Int,
    @Query("language") language: String? = null,
  ): TmdbSeason

  @GET("trending/tv/week")
  suspend fun fetchTrendingShows(
    @Query("page") page: Int,
  ): TmdbPage<TmdbShow>

  @GET("trending/movie/week")
  suspend fun fetchTrendingMovies(
    @Query("page") page: Int,
  ): TmdbPage<TmdbMovie>

  @GET("tv/popular")
  suspend fun fetchPopularShows(
    @Query("page") page: Int,
  ): TmdbPage<TmdbShow>

  @GET("movie/popular")
  suspend fun fetchPopularMovies(
    @Query("page") page: Int,
  ): TmdbPage<TmdbMovie>

  /**
   * TMDB has no "anticipated" feed, so it is approximated with unreleased
   * titles ordered by popularity.
   */
  @GET("discover/tv?sort_by=popularity.desc")
  suspend fun fetchAnticipatedShows(
    @Query("first_air_date.gte") fromDate: String,
    @Query("with_genres") genres: String?,
    @Query("with_watch_providers") providers: String?,
    @Query("watch_region") watchRegion: String?,
    @Query("page") page: Int,
  ): TmdbPage<TmdbShow>

  @GET("discover/movie?sort_by=popularity.desc")
  suspend fun fetchAnticipatedMovies(
    @Query("primary_release_date.gte") fromDate: String,
    @Query("with_genres") genres: String?,
    @Query("with_watch_providers") providers: String?,
    @Query("watch_region") watchRegion: String?,
    @Query("page") page: Int,
  ): TmdbPage<TmdbMovie>

  @GET("tv/{tmdbId}/recommendations")
  suspend fun fetchRelatedShows(
    @Path("tmdbId") tmdbId: Long,
    @Query("page") page: Int,
  ): TmdbPage<TmdbShow>

  @GET("movie/{tmdbId}/recommendations")
  suspend fun fetchRelatedMovies(
    @Path("tmdbId") tmdbId: Long,
    @Query("page") page: Int,
  ): TmdbPage<TmdbMovie>

  /**
   * Used when a genre or streaming filter is active. Trending and popular do
   * not accept filters, so a filtered request has to go through discover
   * instead.
   */
  @GET("discover/tv?sort_by=popularity.desc")
  suspend fun fetchDiscoverShows(
    @Query("with_genres") genres: String?,
    @Query("with_watch_providers") providers: String?,
    @Query("watch_region") watchRegion: String?,
    @Query("page") page: Int,
  ): TmdbPage<TmdbShow>

  @GET("discover/movie?sort_by=popularity.desc")
  suspend fun fetchDiscoverMovies(
    @Query("with_genres") genres: String?,
    @Query("with_watch_providers") providers: String?,
    @Query("watch_region") watchRegion: String?,
    @Query("page") page: Int,
  ): TmdbPage<TmdbMovie>

  @GET("person/{tmdbId}/combined_credits")
  suspend fun fetchPersonCredits(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbPersonCredits

  @GET("collection/{tmdbId}")
  suspend fun fetchCollection(
    @Path("tmdbId") tmdbId: Long,
  ): TmdbCollection

  @GET("search/multi")
  suspend fun fetchSearchResults(
    @Query("query") query: String,
    @Query("page") page: Int,
  ): TmdbPage<TmdbSearchItem>

  @GET("find/{externalId}")
  suspend fun fetchByExternalId(
    @Path("externalId") externalId: String,
    @Query("external_source") source: String,
  ): TmdbFindResult
}
