package xyz.stignarnia.ui_statistics_movies.cases

import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_statistics_movies.views.ratings.recycler.StatisticsMoviesRatingItem
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class StatisticsMoviesLoadRatingsCase @Inject constructor(
  private val moviesRepository: MoviesRepository,
  private val ratingsRepository: RatingsRepository,
  private val imagesProvider: MovieImagesProvider,
) {

  companion object {
    private const val LIMIT = 25
  }

  suspend fun loadRatings(): List<StatisticsMoviesRatingItem> {
    val ratings = ratingsRepository.movies.loadMoviesRatings()
    val ratingsIds = ratings.map { it.idTmdb }
    val myMovies = moviesRepository.myMovies.loadAll(ratingsIds).distinctBy { it.tmdbId }

    return ratings
      .filter { rating -> myMovies.any { it.tmdbId == rating.idTmdb.id } }
      .take(LIMIT)
      .map { rating ->
        val movie = myMovies.first { it.tmdbId == rating.idTmdb.id }
        StatisticsMoviesRatingItem(
          isLoading = false,
          movie = movie,
          image = imagesProvider.findCachedImage(movie, ImageType.POSTER),
          rating = rating,
        )
      }.sortedByDescending { it.rating.ratedAt }
  }
}
