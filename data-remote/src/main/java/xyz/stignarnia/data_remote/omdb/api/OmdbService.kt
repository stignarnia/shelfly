package xyz.stignarnia.data_remote.omdb.api

import xyz.stignarnia.data_remote.omdb.model.OmdbResult
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbService {

  @GET("/")
  suspend fun fetchData(
    @Query("i") imdbId: String,
  ): OmdbResult
}
