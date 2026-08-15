package xyz.stignarnia.data_remote.tmdb

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.data_remote.BuildConfig
import xyz.stignarnia.data_remote.Config
import xyz.stignarnia.data_remote.apikey.ApiKeyProvider
import xyz.stignarnia.data_remote.tmdb.api.TmdbApi
import xyz.stignarnia.data_remote.tmdb.model.TmdbPerson
import xyz.stignarnia.data_remote.tmdb.api.TmdbService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Hits the real TMDB API to prove the mapping against live payloads rather than
 * recorded ones - the only end-to-end check available while the app itself
 * cannot yet run.
 *
 * Skipped unless a TMDB key is compiled in, which is true for local debug builds
 * (prefilled from local.properties) and false on CI, so CI never makes network
 * calls.
 */
class TmdbLiveApiTest {

  private lateinit var api: TmdbApi

  @Before
  fun setUp() {
    assumeTrue("No TMDB key available - skipping live API test", BuildConfig.TMDB_API_KEY.isNotBlank())

    val apiKeyProvider = object : ApiKeyProvider {
      override fun getTmdbApiKey() = BuildConfig.TMDB_API_KEY

      override fun getOmdbApiKey() = BuildConfig.OMDB_API_KEY

      override fun setTmdbApiKey(key: String) = Unit

      override fun setOmdbApiKey(key: String) = Unit

      override fun hasTmdbApiKey() = true

      override fun hasOmdbApiKey() = BuildConfig.OMDB_API_KEY.isNotBlank()
    }

    val okHttpClient = OkHttpClient
      .Builder()
      .addInterceptor(TmdbInterceptor(apiKeyProvider))
      .build()

    val moshi = Moshi
      .Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()

    val service = Retrofit
      .Builder()
      .client(okHttpClient)
      .baseUrl(Config.TMDB_BASE_URL)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(TmdbService::class.java)

    api = TmdbApi(service)
  }

  @Test
  fun `fetches a real show`() =
    runTest {
      val show = api.fetchShow(BREAKING_BAD_TMDB_ID)

      assertThat(show.title).isEqualTo("Breaking Bad")
      assertThat(show.ids?.tmdb).isEqualTo(BREAKING_BAD_TMDB_ID)
      assertThat(show.ids?.imdb).isNotEmpty()
      assertThat(show.year).isEqualTo(2008)
      assertThat(show.genres).isNotEmpty()
      assertThat(show.status).isEqualTo("ended")
      assertThat(show.aired_episodes).isGreaterThan(0)
    }

  @Test
  fun `fetches real seasons with episodes`() =
    runTest {
      val seasons = api.fetchSeasons(BREAKING_BAD_TMDB_ID)

      assertThat(seasons).isNotEmpty()
      val firstSeason = seasons.first { it.number == 1 }
      assertThat(firstSeason.episodes).isNotEmpty()
      assertThat(firstSeason.episodes?.first()?.title).isNotEmpty()
      assertThat(firstSeason.episodes?.first()?.number).isEqualTo(1)
    }

  @Test
  fun `searches for real shows and movies`() =
    runTest {
      val results = api.fetchSearchResults("breaking bad")

      assertThat(results).isNotEmpty()
      assertThat(results.any { it.show?.title == "Breaking Bad" }).isTrue()
    }

  @Test
  fun `fetches real trending shows across pages`() =
    runTest {
      val shows = api.fetchTrendingShows(genres = emptyList(), limit = 30)

      assertThat(shows).hasSize(30)
      assertThat(shows.map { it.ids?.tmdb }.toSet()).hasSize(30)
    }

  @Test
  fun `filters real discover results by genre`() =
    runTest {
      val shows = api.fetchTrendingShows(genres = listOf("animation"), limit = 20)

      assertThat(shows).isNotEmpty()
      // /discover returns full genre lists, so the filter is verifiable here.
      assertThat(shows.all { it.genres?.contains("animation") == true }).isTrue()
    }

  @Test
  fun `fetches a real person's combined credits`() =
    runTest {
      // Bryan Cranston, who has both show and movie credits.
      val credits = api.fetchPersonCredits(17419, TmdbPerson.Type.CAST)

      assertThat(credits).isNotEmpty()
      assertThat(credits.any { it.show != null }).isTrue()
      assertThat(credits.any { it.movie != null }).isTrue()
    }

  @Test
  fun `fetches real localised text for a show`() =
    runTest {
      val translation = api.fetchShowTranslation(BREAKING_BAD_TMDB_ID, "it")

      assertThat(translation?.overview).isNotEmpty()
      assertThat(translation?.language).isEqualTo("it")
    }

  @Test
  fun `fetches real localised episode titles for a season`() =
    runTest {
      val translations = api.fetchSeasonTranslations(BREAKING_BAD_TMDB_ID, 1, "it")

      assertThat(translations).isNotEmpty()
      assertThat(translations.first().number).isEqualTo(1)
      assertThat(
        translations
          .first()
          .translations
          ?.first()
          ?.title,
      ).isNotEmpty()
    }

  @Test
  fun `resolves a real imdb id`() =
    runTest {
      val show = api.fetchShowByImdbId("tt0903747")

      assertThat(show?.ids?.tmdb).isEqualTo(BREAKING_BAD_TMDB_ID)
    }

  companion object {
    private const val BREAKING_BAD_TMDB_ID = 1396L
  }
}
