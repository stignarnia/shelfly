package com.michaldrabik.repository.mappers

import com.michaldrabik.data_local.database.model.Movie
import com.michaldrabik.ui_model.IdImdb
import com.michaldrabik.ui_model.IdSlug
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.IdTvRage
import com.michaldrabik.ui_model.IdTvdb
import com.michaldrabik.ui_model.Ids
import javax.inject.Inject
import com.michaldrabik.data_local.database.model.Show as ShowDb
import com.michaldrabik.data_remote.catalog.model.Ids as IdsNetwork

class IdsMapper @Inject constructor() {

  fun fromNetwork(ids: IdsNetwork?) =
    Ids(
      IdTmdb(ids?.tmdb ?: -1),
      IdSlug(ids?.slug ?: ""),
      IdTvdb(ids?.tvdb ?: -1),
      IdImdb(ids?.imdb ?: ""),
      IdTvRage(ids?.tvrage ?: -1),
    )

  fun toNetwork(ids: Ids?) =
    IdsNetwork(
      slug = ids?.slug?.id,
      tvdb = ids?.tvdb?.id,
      imdb = ids?.imdb?.id,
      tmdb = ids?.tmdb?.id,
      tvrage = ids?.tvrage?.id,
    )

  fun fromDatabase(show: ShowDb?) =
    Ids(
      IdTmdb(show?.idTmdb ?: -1),
      IdSlug(show?.idSlug ?: ""),
      IdTvdb(show?.idTvdb ?: -1),
      IdImdb(show?.idImdb ?: ""),
      IdTvRage(show?.idTvrage ?: -1),
    )

  fun fromDatabase(movie: Movie?) =
    Ids(
      IdTmdb(movie?.idTmdb ?: -1),
      IdSlug(movie?.idSlug ?: ""),
      IdTvdb(-1),
      IdImdb(movie?.idImdb ?: ""),
      IdTvRage(-1),
    )
}
