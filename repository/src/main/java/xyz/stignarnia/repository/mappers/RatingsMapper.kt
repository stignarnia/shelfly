package xyz.stignarnia.repository.mappers

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.database.model.MovieRatings
import xyz.stignarnia.dataLocal.database.model.ShowRatings
import xyz.stignarnia.dataRemote.omdb.model.OmdbResult
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ratings
import javax.inject.Inject

class RatingsMapper
  @Inject
  constructor() {
    fun fromNetwork(omdbResult: OmdbResult) =
      Ratings(
        imdb = if (omdbResult.imdbRating == "N/A") null else Ratings.Value(omdbResult.imdbRating, false),
        metascore = if (omdbResult.Metascore == "N/A") null else Ratings.Value(omdbResult.Metascore, false),
        rottenTomatoes = Ratings.Value(omdbResult.Ratings?.find { it.Source == "Rotten Tomatoes" }?.Value, false),
        rottenTomatoesUrl = if (omdbResult.tomatoURL == "N/A") null else omdbResult.tomatoURL,
      )

    fun fromDatabase(entity: MovieRatings) =
      Ratings(
        tmdb = Ratings.Value(entity.tmdb, false),
        imdb = Ratings.Value(entity.imdb, false),
        rottenTomatoes = Ratings.Value(entity.rottenTomatoes, false),
        rottenTomatoesUrl = entity.rottenTomatoesUrl,
        metascore = Ratings.Value(entity.metascore, false),
      )

    fun fromDatabase(entity: ShowRatings) =
      Ratings(
        tmdb = Ratings.Value(entity.tmdb, false),
        imdb = Ratings.Value(entity.imdb, false),
        rottenTomatoes = Ratings.Value(entity.rottenTomatoes, false),
        rottenTomatoesUrl = entity.rottenTomatoesUrl,
        metascore = Ratings.Value(entity.metascore, false),
      )

    fun toMovieDatabase(
      idTmdb: IdTmdb,
      ratings: Ratings,
    ) = MovieRatings(
      id = 0,
      idTmdb = idTmdb.id,
      tmdb = ratings.tmdb?.value,
      imdb = ratings.imdb?.value,
      metascore = ratings.metascore?.value,
      rottenTomatoes = ratings.rottenTomatoes?.value,
      rottenTomatoesUrl = ratings.rottenTomatoesUrl,
      createdAt = nowUtcMillis(),
      updatedAt = nowUtcMillis(),
    )

    fun toShowDatabase(
      idTmdb: IdTmdb,
      ratings: Ratings,
    ) = ShowRatings(
      id = 0,
      idTmdb = idTmdb.id,
      tmdb = ratings.tmdb?.value,
      imdb = ratings.imdb?.value,
      metascore = ratings.metascore?.value,
      rottenTomatoes = ratings.rottenTomatoes?.value,
      rottenTomatoesUrl = ratings.rottenTomatoesUrl,
      createdAt = nowUtcMillis(),
      updatedAt = nowUtcMillis(),
    )
  }
