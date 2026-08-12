package com.michaldrabik.data_remote.tmdb

import com.google.common.truth.Truth.assertThat
import com.michaldrabik.data_remote.tmdb.api.TmdbApi
import com.michaldrabik.data_remote.tmdb.api.TmdbService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Verifies that TMDB payloads parse and map onto the DTOs the repository layer
 * consumes. These run against recorded responses - see TmdbLiveApiTest for the
 * checks against the real API.
 */
class TmdbApiTest {

  private lateinit var server: MockWebServer
  private lateinit var api: TmdbApi

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()

    val moshi = Moshi
      .Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()

    val service = Retrofit
      .Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(TmdbService::class.java)

    api = TmdbApi(service)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `maps a show payload onto the shared DTO`() =
    runTest {
      server.enqueue(MockResponse().setBody(SHOW_JSON))

      val show = api.fetchShow(1396)

      assertThat(show.ids?.tmdb).isEqualTo(1396)
      assertThat(show.ids?.imdb).isEqualTo("tt0903747")
      assertThat(show.ids?.tvdb).isEqualTo(81189)
      assertThat(show.title).isEqualTo("Breaking Bad")
      assertThat(show.year).isEqualTo(2008)
      assertThat(show.runtime).isEqualTo(45)
      assertThat(show.network).isEqualTo("AMC")
      assertThat(show.country).isEqualTo("us")
      assertThat(show.certification).isEqualTo("TV-MA")
      assertThat(show.aired_episodes).isEqualTo(62)
      assertThat(show.genres).containsExactly("drama", "crime")
      assertThat(show.trailer).isEqualTo("https://youtube.com/watch?v=XZ8daibM3AE")
    }

  @Test
  fun `widens plain dates so they parse as instants`() =
    runTest {
      server.enqueue(MockResponse().setBody(SHOW_JSON))

      val show = api.fetchShow(1396)

      // Trakt returned full instants; TMDB returns "2008-01-20", which
      // ZonedDateTime.parse rejects outright.
      assertThat(show.first_aired).isEqualTo("2008-01-20T00:00:00.000Z")
    }

  @Test
  fun `lowercases status so it matches the ShowStatus keys`() =
    runTest {
      server.enqueue(MockResponse().setBody(SHOW_JSON))

      val show = api.fetchShow(1396)

      assertThat(show.status).isEqualTo("ended")
    }

  @Test
  fun `reports no airtime because TMDB does not expose one`() =
    runTest {
      server.enqueue(MockResponse().setBody(SHOW_JSON))

      val show = api.fetchShow(1396)

      assertThat(show.airs?.day).isNull()
      assertThat(show.airs?.time).isNull()
      assertThat(show.airs?.timezone).isNull()
    }

  @Test
  fun `walks pages until the requested limit is met`() =
    runTest {
      server.enqueue(MockResponse().setBody(pageJson(page = 1, totalPages = 3, ids = listOf(1, 2))))
      server.enqueue(MockResponse().setBody(pageJson(page = 2, totalPages = 3, ids = listOf(3, 4))))

      val shows = api.fetchTrendingShows(genres = emptyList(), limit = 3)

      assertThat(shows).hasSize(3)
      assertThat(shows.map { it.ids?.tmdb }).containsExactly(1L, 2L, 3L).inOrder()
      assertThat(server.requestCount).isEqualTo(2)
    }

  @Test
  fun `deduplicates entries repeated across pages`() =
    runTest {
      // The ranked feeds reorder between requests, so a title can appear twice.
      server.enqueue(MockResponse().setBody(pageJson(page = 1, totalPages = 3, ids = listOf(1, 2))))
      server.enqueue(MockResponse().setBody(pageJson(page = 2, totalPages = 3, ids = listOf(2, 3))))

      val shows = api.fetchTrendingShows(genres = emptyList(), limit = 3)

      assertThat(shows.map { it.ids?.tmdb }).containsExactly(1L, 2L, 3L).inOrder()
    }

  @Test
  fun `stops paging when the results run out`() =
    runTest {
      server.enqueue(MockResponse().setBody(pageJson(page = 1, totalPages = 1, ids = listOf(1))))

      val shows = api.fetchTrendingShows(genres = emptyList(), limit = 50)

      assertThat(shows).hasSize(1)
      assertThat(server.requestCount).isEqualTo(1)
    }

  @Test
  fun `splits multi search results by media type and drops people`() =
    runTest {
      server.enqueue(MockResponse().setBody(SEARCH_JSON))

      val results = api.fetchSearchResults("breaking")

      assertThat(results).hasSize(2)
      assertThat(results[0].show?.title).isEqualTo("Breaking Bad")
      assertThat(results[0].movie).isNull()
      assertThat(results[1].movie?.title).isEqualTo("Breaking In")
      assertThat(results[1].show).isNull()
    }

  @Test
  fun `resolves a show from an imdb id`() =
    runTest {
      server.enqueue(
        MockResponse().setBody("""{"tv_results":[{"id":1396,"name":"Breaking Bad"}],"movie_results":[]}"""),
      )

      val show = api.fetchShowByImdbId("tt0903747")

      assertThat(show?.ids?.tmdb).isEqualTo(1396)
      assertThat(server.takeRequest().path).contains("external_source=imdb_id")
    }

  @Test
  fun `returns null when an imdb id matches nothing`() =
    runTest {
      server.enqueue(MockResponse().setBody("""{"tv_results":[],"movie_results":[]}"""))

      assertThat(api.fetchShowByImdbId("tt0000000")).isNull()
    }

  private fun pageJson(
    page: Int,
    totalPages: Int,
    ids: List<Int>,
  ): String {
    val results = ids.joinToString(",") { """{"id":$it,"name":"Show $it"}""" }
    return """{"page":$page,"total_pages":$totalPages,"total_results":99,"results":[$results]}"""
  }

  companion object {
    private val SHOW_JSON = """
      {
        "id": 1396,
        "name": "Breaking Bad",
        "overview": "A high school chemistry teacher turns to crime.",
        "first_air_date": "2008-01-20",
        "last_air_date": "2013-09-29",
        "episode_run_time": [45, 47],
        "homepage": "http://www.amc.com/shows/breaking-bad",
        "status": "Ended",
        "vote_average": 8.9,
        "vote_count": 12000,
        "number_of_episodes": 62,
        "genres": [{"id": 18, "name": "Drama"}, {"id": 80, "name": "Crime"}],
        "networks": [{"id": 174, "name": "AMC"}],
        "origin_country": ["US"],
        "external_ids": {"imdb_id": "tt0903747", "tvdb_id": 81189},
        "content_ratings": {
          "results": [
            {"iso_3166_1": "DE", "rating": "16"},
            {"iso_3166_1": "US", "rating": "TV-MA"}
          ]
        },
        "videos": {
          "results": [
            {"key": "ignored", "site": "Vimeo", "type": "Trailer"},
            {"key": "XZ8daibM3AE", "site": "YouTube", "type": "Trailer"}
          ]
        }
      }
    """.trimIndent()

    private val SEARCH_JSON = """
      {
        "page": 1,
        "total_pages": 1,
        "total_results": 3,
        "results": [
          {"id": 1396, "media_type": "tv", "name": "Breaking Bad", "first_air_date": "2008-01-20"},
          {"id": 458723, "media_type": "movie", "title": "Breaking In", "release_date": "2018-05-10"},
          {"id": 17419, "media_type": "person", "name": "Bryan Cranston"}
        ]
      }
    """.trimIndent()
  }
}
