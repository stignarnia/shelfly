package xyz.stignarnia.repository.mappers

import xyz.stignarnia.data_local.database.model.Movie
import xyz.stignarnia.ui_model.IdImdb
import xyz.stignarnia.ui_model.IdSlug
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.IdTvRage
import xyz.stignarnia.ui_model.IdTvdb
import xyz.stignarnia.ui_model.Ids
import javax.inject.Inject
import xyz.stignarnia.data_local.database.model.Show as ShowDb
import xyz.stignarnia.data_remote.catalog.model.Ids as IdsNetwork

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
