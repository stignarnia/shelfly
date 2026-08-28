package xyz.stignarnia.repository.mappers

import xyz.stignarnia.dataLocal.database.model.Movie
import xyz.stignarnia.uiModel.IdImdb
import xyz.stignarnia.uiModel.IdSlug
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.IdTvRage
import xyz.stignarnia.uiModel.IdTvdb
import xyz.stignarnia.uiModel.Ids
import javax.inject.Inject
import xyz.stignarnia.dataLocal.database.model.Show as ShowDb
import xyz.stignarnia.dataRemote.catalog.model.Ids as IdsNetwork

class IdsMapper
  @Inject
  constructor() {
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
