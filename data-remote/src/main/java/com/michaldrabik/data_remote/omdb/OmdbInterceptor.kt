package com.michaldrabik.data_remote.omdb

import com.michaldrabik.data_remote.apikey.ApiKeyProvider
import okhttp3.Interceptor
import okhttp3.Response

class OmdbInterceptor(
  private val apiKeyProvider: ApiKeyProvider,
) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val url = chain
      .request()
      .url
      .newBuilder()
      .addQueryParameter("apikey", apiKeyProvider.getOmdbApiKey())
      .addQueryParameter("tomatoes", "true")
      .build()
    val request = chain
      .request()
      .newBuilder()
      .url(url)
      .build()
    return chain.proceed(request)
  }
}
