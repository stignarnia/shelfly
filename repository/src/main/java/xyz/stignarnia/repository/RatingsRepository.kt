package xyz.stignarnia.repository

import xyz.stignarnia.repository.movies.ratings.MoviesRatingsRepository
import xyz.stignarnia.repository.shows.ratings.ShowsRatingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingsRepository @Inject constructor(
  val shows: ShowsRatingsRepository,
  val movies: MoviesRatingsRepository,
)
