package xyz.stignarnia.uiShow.sections.ratings

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
import xyz.stignarnia.uiShow.R
import xyz.stignarnia.uiShow.ShowDetailsViewModel
import xyz.stignarnia.uiShow.databinding.FragmentShowDetailsRatingsBinding

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
          showDetailsRatings.onOmdbKeyMissingClick = { showTip(Tip.RATINGS_OMDB_KEY) }
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
