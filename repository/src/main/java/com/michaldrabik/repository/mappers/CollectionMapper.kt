package com.michaldrabik.repository.mappers

import com.michaldrabik.common.extensions.nowUtc
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.MovieCollection
import java.time.ZonedDateTime
import javax.inject.Inject
import com.michaldrabik.data_local.database.model.MovieCollection as MovieCollectionEntity
import com.michaldrabik.data_remote.trakt.model.MovieCollection as MovieCollectionNetwork

class CollectionMapper @Inject constructor() {

  fun fromNetwork(input: MovieCollectionNetwork): MovieCollection =
    MovieCollection(
      id = IdTmdb(input.ids.tmdb!!),
      name = input.name,
      description = input.description,
      itemCount = input.item_count,
    )

  fun fromEntity(input: MovieCollectionEntity): MovieCollection =
    MovieCollection(
      id = IdTmdb(input.idTmdb),
      name = input.name,
      description = input.description,
      itemCount = input.itemCount,
    )

  fun toEntity(
    movieId: Long,
    input: MovieCollection,
    updatedAt: ZonedDateTime = nowUtc(),
    createdAt: ZonedDateTime = nowUtc(),
  ): MovieCollectionEntity =
    MovieCollectionEntity(
      idTmdb = input.id.id,
      idTmdbMovie = movieId,
      name = input.name,
      description = input.description,
      itemCount = input.itemCount,
      updatedAt = updatedAt,
      createdAt = createdAt,
    )
}
