package xyz.stignarnia.data_remote.tmdb

import xyz.stignarnia.data_remote.apikey.ApiKeyProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Authenticates against TMDB with a v3 API key passed as a query parameter.
 *
 * The v4 "Authorization: Bearer" scheme expects a read access token instead, which is not the credential users are given when they create a TMDB key.
 */
class TmdbInterceptor(
  private val apiKeyProvider: ApiKeyProvider,
) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val url = chain
      .request()
      .url
      .newBuilder()
      .addQueryParameter("api_key", apiKeyProvider.getTmdbApiKey())
      .build()

    val request = chain
      .request()
      .newBuilder()
      .url(url)
      .header("Content-Type", "application/json")
      .build()

    return chain.proceed(request)
  }
}
