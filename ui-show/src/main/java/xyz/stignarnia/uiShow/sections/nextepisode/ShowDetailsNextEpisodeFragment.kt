package xyz.stignarnia.uiShow.sections.nextepisode

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.common.Config.SPOILERS_HIDE_SYMBOL
import xyz.stignarnia.common.Config.SPOILERS_REGEX
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.capitalizeWords
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiEpisodes.details.EpisodeDetailsBottomSheet
import xyz.stignarnia.uiShow.R
import xyz.stignarnia.uiShow.ShowDetailsFragment
import xyz.stignarnia.uiShow.ShowDetailsViewModel
import xyz.stignarnia.uiShow.databinding.FragmentShowDetailsNextEpisodeBinding
import xyz.stignarnia.uiShow.sections.nextepisode.helpers.NextEpisodeBundle
import java.util.Locale

@AndroidEntryPoint
class ShowDetailsNextEpisodeFragment :
  BaseFragment<ShowDetailsNextEpisodeViewModel>(
    R.layout.fragment_show_details_next_episode,
  ) {
  override val navigationId = R.id.showDetailsFragment
  private val binding by viewBinding(FragmentShowDetailsNextEpisodeBinding::bind)

  private val parentViewModel by viewModels<ShowDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ShowDetailsNextEpisodeViewModel>()

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    launchAndRepeatStarted(
      { parentViewModel.parentShowState.collect { it?.let { viewModel.loadNextEpisode(it) } } },
      { viewModel.uiState.collect { render(it) } },
    )
  }

  private fun render(uiState: ShowDetailsNextEpisodeUiState) {
    with(uiState) {
      with(binding) {
        nextEpisode?.let { episodeBundle ->
          val episode = episodeBundle.nextEpisode.second

          var episodeTitle = episode.title
          if (!episodeBundle.isWatched && spoilersSettings?.isEpisodeTitleHidden == true) {
            showDetailsEpisodeText.tag = episodeTitle
            episodeTitle = SPOILERS_REGEX.replace(episodeTitle, SPOILERS_HIDE_SYMBOL)

            if (spoilersSettings.isTapToReveal) {
              showDetailsEpisodeText.onClick { view ->
                view.tag?.let {
                  showDetailsEpisodeText.text =
                    String.format(
                      Locale.ENGLISH,
                      getString(R.string.textEpisodeTitle),
                      episode.season,
                      episode.number,
                      it.toString(),
                    )
                }
                view.isClickable = false
              }
            }
          }

          showDetailsEpisodeText.text =
            String.format(
              Locale.ENGLISH,
              getString(R.string.textEpisodeTitle),
              episode.season,
              episode.number,
              episodeTitle,
            )

          episode.firstAired?.let { date ->
            val displayDate = episodeBundle.dateFormat?.format(date.toLocalZone())?.capitalizeWords()
            showDetailsEpisodeAirtime.visible()
            showDetailsEpisodeAirtime.text = displayDate
          }

          showDetailsEpisodeRoot.onClick { openDetails(episodeBundle) }
          (requireParentFragment() as ShowDetailsFragment)
            .binding.showDetailsEpisodeFragment
            .fadeIn(withHardware = true)
        }
      }
    }
  }

  private fun openDetails(episodeBundle: NextEpisodeBundle) {
    val (show, episode) = episodeBundle.nextEpisode
    val bundle =
      EpisodeDetailsBottomSheet.createBundle(
        showIds = show.ids,
        episode = episode,
        seasonEpisodesIds = null,
        isWatched = episodeBundle.isWatched,
        showTabs = false,
      )
    navigateToSafe(R.id.actionShowDetailsFragmentEpisodeDetails, bundle)
  }

  override fun setupBackPressed() = Unit
}
