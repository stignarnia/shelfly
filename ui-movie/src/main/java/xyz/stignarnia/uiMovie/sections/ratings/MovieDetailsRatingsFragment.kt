package xyz.stignarnia.uiMovie.sections.ratings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.AppCountry
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.openImdbUrl
import xyz.stignarnia.uiBase.utilities.extensions.openWebUrl
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.IdImdb
import xyz.stignarnia.uiModel.Tip
import xyz.stignarnia.uiMovie.MovieDetailsViewModel
import xyz.stignarnia.uiMovie.R
import xyz.stignarnia.uiMovie.databinding.FragmentMovieDetailsRatingsBinding
import xyz.stignarnia.uiMovie.helpers.MovieLink

@AndroidEntryPoint
class MovieDetailsRatingsFragment :
  BaseFragment<MovieDetailsRatingsViewModel>(
    R.layout.fragment_movie_details_ratings,
  ) {
  private val parentViewModel by viewModels<MovieDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<MovieDetailsRatingsViewModel>()
  private val binding by viewBinding(FragmentMovieDetailsRatingsBinding::bind)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    launchAndRepeatStarted(
      { parentViewModel.parentMovieState.collect { it?.let { viewModel.loadRatings(it) } } },
      { parentViewModel.parentFollowedState.collect { it?.let { viewModel.refreshRatings() } } },
      { viewModel.uiState.collect { render(it) } },
    )
  }

  private fun render(uiState: MovieDetailsRatingsUiState) {
    with(uiState) {
      with(binding) {
        ratings?.let {
          if (movieDetailsRatings.isBound() && !isRefreshingRatings) {
            return
          }
          movieDetailsRatings.bind(ratings)
          movieDetailsRatings.onOmdbKeyMissingClick = { showTip(Tip.RATINGS_OMDB_KEY) }
          movie?.let {
            movieDetailsRatings.onTmdbClick = { openMovieLink(MovieLink.TMDB, movie.tmdbId.toString()) }
            movieDetailsRatings.onImdbClick = { openMovieLink(MovieLink.IMDB, movie.ids.imdb.id) }
            movieDetailsRatings.onMetaClick = { openMovieLink(MovieLink.METACRITIC, movie.title) }
            movieDetailsRatings.onRottenClick = {
              val url = it.rottenTomatoesUrl
              if (!url.isNullOrBlank()) {
                openWebUrl(url) ?: openMovieLink(MovieLink.ROTTEN, "${movie.title} ${movie.year}")
              } else {
                openMovieLink(MovieLink.ROTTEN, "${movie.title} ${movie.year}")
              }
            }
          }
        }
      }
    }
  }

  private fun openMovieLink(
    link: MovieLink,
    id: String,
    country: AppCountry = AppCountry.UNITED_STATES,
  ) {
    if (link == MovieLink.IMDB) {
      openImdbUrl(IdImdb(id)) ?: showSnack(MessageEvent.Info(R.string.errorCouldNotFindApp))
    } else {
      openWebUrl(link.getUri(id, country)) ?: showSnack(MessageEvent.Info(R.string.errorCouldNotFindApp))
    }
  }

  override fun setupBackPressed() = Unit
}
