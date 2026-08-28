package xyz.stignarnia.uiStatisticsMovies.views.ratings.recycler

import xyz.stignarnia.uiBase.common.MovieListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.UserRating

data class StatisticsMoviesRatingItem(
  override val movie: Movie,
  override val image: Image,
  override val isLoading: Boolean,
  val rating: UserRating,
) : MovieListItem
