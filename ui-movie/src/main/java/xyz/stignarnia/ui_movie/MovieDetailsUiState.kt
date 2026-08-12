package xyz.stignarnia.ui_movie

import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.RatingState
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_movie.helpers.MovieDetailsMeta
import java.time.ZonedDateTime

data class MovieDetailsUiState(
  val movie: Movie? = null,
  val movieLoading: Boolean? = null,
  val image: Image? = null,
  val listsCount: Int? = null,
  val followedState: FollowedState? = null,
  val ratingState: RatingState? = null,
  val translation: Translation? = null,
  val meta: MovieDetailsMeta? = null,
  val spoilers: SpoilersSettings? = null,
) {

  data class FollowedState(
    val isMyMovie: Boolean,
    val isWatchlist: Boolean,
    val isHidden: Boolean,
    val withAnimation: Boolean,
    val watchedAt: ZonedDateTime? = null,
  ) {

    fun isInCollection() = isMyMovie || isWatchlist || isHidden

    companion object {
      fun idle() = FollowedState(isMyMovie = false, isWatchlist = false, isHidden = false, withAnimation = true)

      fun inMyMovies() = FollowedState(isMyMovie = true, isWatchlist = false, isHidden = false, withAnimation = true)

      fun inHidden() = FollowedState(isMyMovie = false, isWatchlist = false, isHidden = true, withAnimation = true)

      fun inWatchlist() = FollowedState(isMyMovie = false, isWatchlist = true, isHidden = false, withAnimation = true)
    }
  }
}
