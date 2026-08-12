package com.michaldrabik.data_remote.tmdb.api

import com.michaldrabik.data_remote.tmdb.TmdbGenres
import com.michaldrabik.data_remote.tmdb.TmdbRemoteDataSource
import com.michaldrabik.data_remote.tmdb.model.TmdbImages
import com.michaldrabik.data_remote.tmdb.model.TmdbPage
import com.michaldrabik.data_remote.tmdb.model.TmdbPerson
import com.michaldrabik.data_remote.tmdb.model.TmdbStreamingCountry
import com.michaldrabik.data_remote.tmdb.model.TmdbTranslation
import com.michaldrabik.data_remote.tmdb.toEpisode
import com.michaldrabik.data_remote.tmdb.toMovie
import com.michaldrabik.data_remote.tmdb.toSeason
import com.michaldrabik.data_remote.tmdb.toShow
import com.michaldrabik.data_remote.trakt.model.Episode
import com.michaldrabik.data_remote.trakt.model.Ids
import com.michaldrabik.data_remote.trakt.model.Movie
import com.michaldrabik.data_remote.trakt.model.PersonCredit
import com.michaldrabik.data_remote.trakt.model.SearchResult
import com.michaldrabik.data_remote.trakt.model.Season
import com.michaldrabik.data_remote.trakt.model.SeasonTranslation
import com.michaldrabik.data_remote.trakt.model.Show
import com.michaldrabik.data_remote.trakt.model.Translation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.ZoneOffset

internal class TmdbApi(
  private val service: TmdbService,
) : TmdbRemoteDataSource {

  override suspend fun fetchShowImages(tmdbId: Long) =
    try {
      if (tmdbId <= 0) TmdbImages.EMPTY
      service.fetchShowImages(tmdbId)
    } catch (error: Throwable) {
      TmdbImages.EMPTY
    }

  override suspend fun fetchEpisodeImage(
    showTmdbId: Long?,
    season: Int?,
    episode: Int?,
  ) = try {
    if (showTmdbId == null || showTmdbId <= 0) TmdbImages.EMPTY
    if (season == null || season <= 0) TmdbImages.EMPTY
    if (episode == null || episode <= 0) TmdbImages.EMPTY
    val images = service.fetchEpisodeImages(showTmdbId, season, episode)
    images.stills?.firstOrNull()
  } catch (error: Throwable) {
    null
  }

  override suspend fun fetchMovieImages(tmdbId: Long) =
    try {
      if (tmdbId <= 0) TmdbImages.EMPTY
      service.fetchMovieImages(tmdbId)
    } catch (error: Throwable) {
      TmdbImages.EMPTY
    }

  override suspend fun fetchMoviePeople(tmdbId: Long): Map<TmdbPerson.Type, List<TmdbPerson>> {
    val result = service.fetchMoviePeople(tmdbId)
    val cast = result.cast?.toList() ?: emptyList()
    val crew = result.crew?.toList() ?: emptyList()
    return mapOf(
      TmdbPerson.Type.CAST to cast,
      TmdbPerson.Type.CREW to crew,
    )
  }

  override suspend fun fetchShowPeople(tmdbId: Long): Map<TmdbPerson.Type, List<TmdbPerson>> {
    val result = service.fetchShowPeople(tmdbId)
    val cast = result.cast?.toList() ?: emptyList()
    val crew = result.crew?.toList() ?: emptyList()
    return mapOf(
      TmdbPerson.Type.CAST to cast,
      TmdbPerson.Type.CREW to crew,
    )
  }

  override suspend fun fetchShowWatchProviders(
    tmdbId: Long,
    countryCode: String,
  ): TmdbStreamingCountry? {
    val result = service.fetchShowWatchProviders(tmdbId)
    val code = when (countryCode.uppercase()) {
      "UK" -> "GB"
      else -> countryCode.uppercase()
    }
    return result.results[code]
  }

  override suspend fun fetchMovieWatchProviders(
    tmdbId: Long,
    countryCode: String,
  ): TmdbStreamingCountry? {
    val result = service.fetchMovieWatchProviders(tmdbId)
    val code = when (countryCode.uppercase()) {
      "UK" -> "GB"
      else -> countryCode.uppercase()
    }
    return result.results[code]
  }

  override suspend fun fetchPersonDetails(id: Long): TmdbPerson = service.fetchPersonDetails(id)

  override suspend fun fetchPersonTranslations(id: Long): Map<String, TmdbTranslation.Data> {
    val result = service.fetchPersonTranslation(id).translations ?: emptyList()
    return result
      .filter {
        if (it.iso_639_1.lowercase() != "zh") true else it.iso_3166_1.lowercase() == "cn"
      } // Chinese Simplified filter
      .associateBy(
        keySelector = { it.iso_639_1.lowercase() },
        valueTransform = { it.data ?: TmdbTranslation.Data(null) },
      )
  }

  override suspend fun fetchPersonImages(tmdbId: Long) =
    try {
      if (tmdbId <= 0) TmdbImages.EMPTY
      service.fetchPersonImages(tmdbId)
    } catch (error: Throwable) {
      TmdbImages.EMPTY
    }

  override suspend fun fetchShow(
    tmdbId: Long,
    language: String?,
  ): Show = service.fetchShow(tmdbId, language).toShow()

  override suspend fun fetchMovie(
    tmdbId: Long,
    language: String?,
  ): Movie = service.fetchMovie(tmdbId, language).toMovie()

  /**
   * A show's episodes are only available per season, so this fans out over the
   * season list from the show payload. Season 0 (specials) is included, matching
   * what Trakt returned.
   */
  override suspend fun fetchSeasons(tmdbId: Long): List<Season> =
    coroutineScope {
      val show = service.fetchShow(tmdbId, null)
      val seasonNumbers = show.seasons?.mapNotNull { it.season_number } ?: emptyList()
      seasonNumbers
        .map { number ->
          async { service.fetchSeason(tmdbId, number).toSeason() }
        }.awaitAll()
    }

  // Trending and popular accept no filters, so a genre filter routes the
  // request through discover instead.

  override suspend fun fetchTrendingShows(
    genres: List<String>,
    limit: Int,
  ): List<Show> {
    val query = TmdbGenres.showQuery(genres)
    return fetchPaged(limit, { it.id }) {
      if (query != null) service.fetchDiscoverShows(query, it) else service.fetchTrendingShows(it)
    }.map { it.toShow() }
  }

  override suspend fun fetchTrendingMovies(
    genres: List<String>,
    limit: Int,
  ): List<Movie> {
    val query = TmdbGenres.movieQuery(genres)
    return fetchPaged(limit, { it.id }) {
      if (query != null) service.fetchDiscoverMovies(query, it) else service.fetchTrendingMovies(it)
    }.map { it.toMovie() }
  }

  override suspend fun fetchPopularShows(
    genres: List<String>,
    limit: Int,
  ): List<Show> {
    val query = TmdbGenres.showQuery(genres)
    return fetchPaged(limit, { it.id }) {
      if (query != null) service.fetchDiscoverShows(query, it) else service.fetchPopularShows(it)
    }.map { it.toShow() }
  }

  override suspend fun fetchPopularMovies(
    genres: List<String>,
    limit: Int,
  ): List<Movie> {
    val query = TmdbGenres.movieQuery(genres)
    return fetchPaged(limit, { it.id }) {
      if (query != null) service.fetchDiscoverMovies(query, it) else service.fetchPopularMovies(it)
    }.map { it.toMovie() }
  }

  override suspend fun fetchAnticipatedShows(
    genres: List<String>,
    limit: Int,
  ): List<Show> = fetchPaged(limit, { it.id }) { service.fetchAnticipatedShows(today(), it) }.map { it.toShow() }

  override suspend fun fetchAnticipatedMovies(
    genres: List<String>,
    limit: Int,
  ): List<Movie> = fetchPaged(limit, { it.id }) { service.fetchAnticipatedMovies(today(), it) }.map { it.toMovie() }

  /**
   * TMDB carries the next episode on the show payload rather than on a
   * dedicated endpoint.
   */
  override suspend fun fetchNextEpisode(tmdbId: Long): Episode? =
    service
      .fetchShow(tmdbId, null)
      .next_episode_to_air
      ?.toEpisode()

  override suspend fun fetchPersonCredits(
    tmdbId: Long,
    type: TmdbPerson.Type,
  ): List<PersonCredit> {
    val credits = service.fetchPersonCredits(tmdbId)
    val items = when (type) {
      TmdbPerson.Type.CAST -> credits.cast
      TmdbPerson.Type.CREW -> credits.crew
    }
    return items
      .orEmpty()
      .filter { it.isShow() || it.isMovie() }
      .map {
        PersonCredit(
          characters = null,
          episode_count = null,
          series_regular = null,
          show = if (it.isShow()) it.toShow() else null,
          movie = if (it.isMovie()) it.toMovie() else null,
        )
      }
  }

  /**
   * TMDB serves localised text by asking for the resource in that language
   * rather than through a separate translations endpoint. A title that comes
   * back identical to the default is treated as untranslated.
   */
  override suspend fun fetchShowTranslation(
    tmdbId: Long,
    language: String,
  ): Translation? {
    val show = service.fetchShow(tmdbId, language)
    return Translation(
      title = show.name,
      overview = show.overview,
      language = language,
      country = null,
    )
  }

  override suspend fun fetchMovieTranslation(
    tmdbId: Long,
    language: String,
  ): Translation? {
    val movie = service.fetchMovie(tmdbId, language)
    return Translation(
      title = movie.title,
      overview = movie.overview,
      language = language,
      country = null,
    )
  }

  override suspend fun fetchSeasonTranslations(
    tmdbId: Long,
    seasonNumber: Int,
    language: String,
  ): List<SeasonTranslation> {
    val season = service.fetchSeason(tmdbId, seasonNumber, language)
    return season.episodes.orEmpty().map { episode ->
      SeasonTranslation(
        season = episode.season_number ?: seasonNumber,
        number = episode.episode_number ?: -1,
        ids = Ids(
          trakt = null,
          slug = null,
          tvdb = null,
          imdb = null,
          tmdb = episode.id,
          tvrage = null,
        ),
        translations = listOf(
          Translation(
            title = episode.name,
            overview = episode.overview,
            language = language,
            country = null,
          ),
        ),
      )
    }
  }

  override suspend fun fetchRelatedShows(tmdbId: Long): List<Show> =
    service
      .fetchRelatedShows(tmdbId, 1)
      .results
      ?.map { it.toShow() }
      ?: emptyList()

  override suspend fun fetchRelatedMovies(tmdbId: Long): List<Movie> =
    service
      .fetchRelatedMovies(tmdbId, 1)
      .results
      ?.map { it.toMovie() }
      ?: emptyList()

  override suspend fun fetchSearchResults(query: String): List<SearchResult> =
    service
      .fetchSearchResults(query, 1)
      .results
      .orEmpty()
      .filter { it.isShow() || it.isMovie() }
      .mapIndexed { index, item ->
        SearchResult(
          order = index,
          score = item.vote_average,
          show = if (item.isShow()) item.toShow() else null,
          movie = if (item.isMovie()) item.toMovie() else null,
          person = null,
        )
      }

  override suspend fun fetchShowByImdbId(imdbId: String): Show? =
    service
      .fetchByExternalId(imdbId, EXTERNAL_SOURCE_IMDB)
      .tv_results
      ?.firstOrNull()
      ?.toShow()

  override suspend fun fetchMovieByImdbId(imdbId: String): Movie? =
    service
      .fetchByExternalId(imdbId, EXTERNAL_SOURCE_IMDB)
      .movie_results
      ?.firstOrNull()
      ?.toMovie()

  /**
   * TMDB pages every list endpoint at 20 items, so a longer list means walking
   * pages until the caller's limit is met or the results run out.
   *
   * Entries are deduplicated by id as they accumulate: the ranked feeds reorder
   * between requests, so the same title can legitimately appear on two pages and
   * would otherwise show up twice in the list.
   */
  private suspend fun <T> fetchPaged(
    limit: Int,
    key: (T) -> Any?,
    fetch: suspend (Int) -> TmdbPage<T>,
  ): List<T> {
    val results = LinkedHashMap<Any?, T>()
    var page = 1
    while (results.size < limit) {
      val response = fetch(page)
      val items = response.results.orEmpty()
      if (items.isEmpty()) {
        break
      }
      items.forEach { item ->
        val itemKey = key(item)
        if (!results.containsKey(itemKey)) {
          results[itemKey] = item
        }
      }
      if (page >= (response.total_pages ?: page)) {
        break
      }
      page++
    }
    return results.values.take(limit)
  }

  private fun today(): String = LocalDate.now(ZoneOffset.UTC).toString()

  companion object {
    private const val EXTERNAL_SOURCE_IMDB = "imdb_id"
  }
}
