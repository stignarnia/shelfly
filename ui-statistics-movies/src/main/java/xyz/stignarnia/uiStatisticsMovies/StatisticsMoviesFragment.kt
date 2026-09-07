package xyz.stignarnia.uiStatisticsMovies

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
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.uiStatisticsMovies.databinding.FragmentStatisticsMoviesBinding

@AndroidEntryPoint
class StatisticsMoviesFragment : BaseFragment<StatisticsMoviesViewModel>(R.layout.fragment_statistics_movies) {
  override val viewModel by viewModels<StatisticsMoviesViewModel>()
  private val binding by viewBinding(FragmentStatisticsMoviesBinding::bind)

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
          loadData(initialDelay = if (!isInitialized) 150L else 0L)
          loadRatings()
          isInitialized = true
        }
      }
    }
  }

  private fun setupView() {
    with(binding) {
      statisticsMoviesToolbar.setOnClickListener { navigateBack() }
      statisticsMoviesRatings.run {
        onMovieClickListener = {
          openMovieDetails(it.movie.tmdbId)
        }
        onMissingImageListener = { item, force ->
          viewModel.loadMissingRatingImage(item, force)
        }
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

  private fun render(uiState: StatisticsMoviesUiState) {
    uiState.run {
      with(binding) {
        statisticsMoviesTotalTimeSpent.bind(totalTimeSpentMinutes ?: 0)
        statisticsMoviesTotalMovies.bind(totalWatchedMovies ?: 0)
        statisticsMoviesTopGenres.bind(topGenres ?: emptyList())
        statisticsMoviesRatings.bind(ratings ?: emptyList())
        ratings?.let { statisticsMoviesRatings.visibleIf(it.isNotEmpty()) }
        totalWatchedMovies?.let {
          statisticsMoviesContent.fadeIf(it > 0)
          statisticsMoviesEmptyView.root.fadeIf(it <= 0)
        }
      }
    }
  }

  private fun openMovieDetails(tmdbId: Long) {
    val bundle = Bundle().apply { putLong(ARG_MOVIE_ID, tmdbId) }
    navigateTo(R.id.actionStatisticsMoviesFragmentToMovieDetailsFragment, bundle)
  }
}
