package xyz.stignarnia.ui_show.sections.ratings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.common.AppCountry
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.openImdbUrl
import xyz.stignarnia.ui_base.utilities.extensions.openWebUrl
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.IdImdb
import xyz.stignarnia.ui_show.R
import xyz.stignarnia.ui_show.ShowDetailsViewModel
import xyz.stignarnia.ui_show.databinding.FragmentShowDetailsRatingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowDetailsRatingsFragment : BaseFragment<ShowDetailsRatingsViewModel>(R.layout.fragment_show_details_ratings) {

  override val navigationId = R.id.showDetailsFragment
  private val binding by viewBinding(FragmentShowDetailsRatingsBinding::bind)

  private val parentViewModel by viewModels<ShowDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ShowDetailsRatingsViewModel>()

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    launchAndRepeatStarted(
      { parentViewModel.parentShowState.collect { it?.let { viewModel.loadRatings(it) } } },
      { parentViewModel.parentFollowedState.collect { it?.let { viewModel.refreshRatings() } } },
      { viewModel.uiState.collect { render(it) } },
    )
  }

  private fun render(uiState: ShowDetailsRatingsUiState) {
    with(uiState) {
      with(binding) {
        ratings?.let {
          if (showDetailsRatings.isBound() && !isRefreshingRatings) {
            return
          }
          showDetailsRatings.bind(ratings)
          show?.let {
            showDetailsRatings.onTmdbClick = { openLink(ShowLink.TMDB, show.tmdbId.toString()) }
            showDetailsRatings.onImdbClick = { openLink(ShowLink.IMDB, show.ids.imdb.id) }
            showDetailsRatings.onMetaClick = { openLink(ShowLink.METACRITIC, show.title) }
            showDetailsRatings.onRottenClick = {
              val url = it.rottenTomatoesUrl
              if (!url.isNullOrBlank()) {
                openWebUrl(url) ?: openLink(ShowLink.ROTTEN, "${show.title} ${show.year}")
              } else {
                openLink(ShowLink.ROTTEN, "${show.title} ${show.year}")
              }
            }
          }
        }
      }
    }
  }

  private fun openLink(
    link: ShowLink,
    id: String,
    country: AppCountry = AppCountry.UNITED_STATES,
  ) {
    if (link == ShowLink.IMDB) {
      openImdbUrl(IdImdb(id)) ?: showSnack(MessageEvent.Info(R.string.errorCouldNotFindApp))
    } else {
      openWebUrl(link.getUri(id, country)) ?: showSnack(MessageEvent.Info(R.string.errorCouldNotFindApp))
    }
  }

  override fun setupBackPressed() = Unit
}
