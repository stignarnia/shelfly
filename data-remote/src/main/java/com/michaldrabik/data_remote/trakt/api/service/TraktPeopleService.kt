package com.michaldrabik.data_remote.trakt.api.service

import com.michaldrabik.data_remote.trakt.model.PersonCreditsResult
import retrofit2.http.GET
import retrofit2.http.Path

interface TraktPeopleService {

  @GET("people/{tmdbId}/{type}?extended=full")
  suspend fun fetchPersonCredits(
    @Path("tmdbId") tmdbId: Long,
    @Path("type") type: String,
  ): PersonCreditsResult
}
