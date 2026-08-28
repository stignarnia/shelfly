package xyz.stignarnia.dataRemote.omdb

import okhttp3.Interceptor
import okhttp3.Response
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider

class OmdbInterceptor(
  private val apiKeyProvider: ApiKeyProvider,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val url =
      chain
        .request()
        .url
        .newBuilder()
        .addQueryParameter("apikey", apiKeyProvider.getOmdbApiKey())
        .addQueryParameter("tomatoes", "true")
        .build()
    val request =
      chain
        .request()
        .newBuilder()
        .url(url)
        .build()
    return chain.proceed(request)
  }
}
