package xyz.stignarnia.dataRemote.omdb.api

import retrofit2.http.GET
import retrofit2.http.Query
import xyz.stignarnia.dataRemote.omdb.model.OmdbResult

interface OmdbService {
  @GET("/")
  suspend fun fetchData(
    @Query("i") imdbId: String,
  ): OmdbResult
}
