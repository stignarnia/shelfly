package xyz.stignarnia.ui_statistics_movies.views.ratings.recycler

import xyz.stignarnia.ui_base.common.MovieListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.UserRating

data class StatisticsMoviesRatingItem(
  override val movie: Movie,
  override val image: Image,
  override val isLoading: Boolean,
  val rating: UserRating,
) : MovieListItem
