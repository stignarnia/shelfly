package xyz.stignarnia.repository.mappers

import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.MovieCollection
import java.time.ZonedDateTime
import javax.inject.Inject
import xyz.stignarnia.data_local.database.model.MovieCollection as MovieCollectionEntity
import xyz.stignarnia.data_remote.catalog.model.MovieCollection as MovieCollectionNetwork

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
