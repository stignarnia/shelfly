package xyz.stignarnia.uiStatistics

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.navigateBack
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.uiStatistics.databinding.FragmentStatisticsBinding

@AndroidEntryPoint
class StatisticsFragment : BaseFragment<StatisticsViewModel>(R.layout.fragment_statistics) {
  override val viewModel by viewModels<StatisticsViewModel>()

  private val binding by viewBinding(FragmentStatisticsBinding::bind)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupInsets()

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        with(viewModel) {
          launch { uiState.collect { render(it) } }
          if (!isInitialized) {
            loadData()
            isInitialized = true
          }
          loadRatings()
        }
      }
    }
  }

  private fun setupView() {
    with(binding) {
      statisticsToolbar.setOnClickListener { navigateBack() }
      statisticsMostWatchedShows.run {
        onLoadMoreClickListener = { addLimit -> viewModel.loadData(addLimit) }
        onShowClickListener = {
          openShowDetails(it.tmdbId)
        }
      }
      statisticsRatings.onShowClickListener = {
        openShowDetails(it.show.tmdbId)
      }
    }
  }

  private fun setupInsets() {
    binding.root.doOnApplyWindowInsets { view, insets, padding, _ ->
      val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(
        top = padding.top + inset.top,
        bottom = padding.bottom + inset.bottom,
      )
    }
  }

  private fun render(uiState: StatisticsUiState) {
    uiState.run {
      with(binding) {
        statisticsMostWatchedShows.bind(mostWatchedShows ?: emptyList(), mostWatchedTotalCount ?: 0)
        statisticsTotalTimeSpent.bind(totalTimeSpentMinutes ?: 0)
        statisticsTotalEpisodes.bind(totalWatchedEpisodes ?: 0, totalWatchedEpisodesShows ?: 0)
        statisticsTopGenres.bind(topGenres ?: emptyList())
        statisticsRatings.bind(ratings ?: emptyList())

        ratings?.let { statisticsRatings.visibleIf(it.isNotEmpty()) }
        mostWatchedShows?.let {
          statisticsContent.fadeIf(it.isNotEmpty())
          statisticsEmptyView.rootLayout.fadeIf(it.isEmpty())
        }
      }
    }
  }

  private fun openShowDetails(tmdbId: Long) {
    val bundle = Bundle().apply { putLong(ARG_SHOW_ID, tmdbId) }
    navigateTo(R.id.actionStatisticsFragmentToShowDetailsFragment, bundle)
  }
}
