package xyz.stignarnia.repository.mappers

import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.dataLocal.database.model.MovieStreaming
import xyz.stignarnia.dataLocal.database.model.ShowStreaming
import xyz.stignarnia.dataRemote.tmdb.model.TmdbStreamingCountry
import xyz.stignarnia.dataRemote.tmdb.model.TmdbStreamingService
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.StreamingService
import xyz.stignarnia.uiModel.StreamingService.Option.ADS
import xyz.stignarnia.uiModel.StreamingService.Option.BUY
import xyz.stignarnia.uiModel.StreamingService.Option.FLATRATE
import xyz.stignarnia.uiModel.StreamingService.Option.FREE
import xyz.stignarnia.uiModel.StreamingService.Option.RENT
import javax.inject.Inject

class StreamingsMapper
  @Inject
  constructor() {
    fun fromDatabaseShow(
      input: List<ShowStreaming>,
      mediaName: String,
      countryCode: String,
    ): List<StreamingService> =
      input.map {
        StreamingService(
          imagePath = it.logoPath ?: "",
          name = it.providerName ?: "",
          options = listOf(StreamingService.Option.valueOf(it.type!!)),
          mediaName = mediaName,
          countryCode = countryCode,
          link = it.link ?: "",
        )
      }

    fun toDatabaseShow(
      ids: Ids,
      input: TmdbStreamingCountry,
    ) = mutableListOf<ShowStreaming>().apply {
      addAll(input.flatrate?.map { createEntityShow(ids, FLATRATE, input, it) } ?: emptyList())
      addAll(input.free?.map { createEntityShow(ids, FREE, input, it) } ?: emptyList())
      addAll(input.buy?.map { createEntityShow(ids, BUY, input, it) } ?: emptyList())
      addAll(input.rent?.map { createEntityShow(ids, RENT, input, it) } ?: emptyList())
      addAll(input.ads?.map { createEntityShow(ids, ADS, input, it) } ?: emptyList())
    }

    fun fromDatabaseMovie(
      input: List<MovieStreaming>,
      mediaName: String,
      countryCode: String,
    ): List<StreamingService> =
      input.map {
        StreamingService(
          imagePath = it.logoPath ?: "",
          name = it.providerName ?: "",
          options = listOf(StreamingService.Option.valueOf(it.type!!)),
          mediaName = mediaName,
          countryCode = countryCode,
          link = it.link ?: "",
        )
      }

    fun toDatabaseMovie(
      ids: Ids,
      input: TmdbStreamingCountry,
    ) = mutableListOf<MovieStreaming>().apply {
      addAll(input.flatrate?.map { createEntityMovie(ids, FLATRATE, input, it) } ?: emptyList())
      addAll(input.free?.map { createEntityMovie(ids, FREE, input, it) } ?: emptyList())
      addAll(input.buy?.map { createEntityMovie(ids, BUY, input, it) } ?: emptyList())
      addAll(input.rent?.map { createEntityMovie(ids, RENT, input, it) } ?: emptyList())
      addAll(input.ads?.map { createEntityMovie(ids, ADS, input, it) } ?: emptyList())
    }

    private fun createEntityMovie(
      ids: Ids,
      option: StreamingService.Option,
      country: TmdbStreamingCountry,
      input: TmdbStreamingService,
    ) = MovieStreaming(
      idTmdb = ids.tmdb.id,
      type = option.name,
      providerId = input.provider_id,
      providerName = input.provider_name,
      displayPriority = input.display_priority,
      logoPath = input.logo_path,
      link = country.link,
      createdAt = nowUtc(),
      updatedAt = nowUtc(),
    )

    private fun createEntityShow(
      ids: Ids,
      option: StreamingService.Option,
      country: TmdbStreamingCountry,
      input: TmdbStreamingService,
    ) = ShowStreaming(
      idTmdb = ids.tmdb.id,
      type = option.name,
      providerId = input.provider_id,
      providerName = input.provider_name,
      displayPriority = input.display_priority,
      logoPath = input.logo_path,
      link = country.link,
      createdAt = nowUtc(),
      updatedAt = nowUtc(),
    )
  }
