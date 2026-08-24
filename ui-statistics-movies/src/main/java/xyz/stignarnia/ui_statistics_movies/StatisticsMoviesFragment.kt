package xyz.stignarnia.ui_statistics_movies

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.fadeIf
import xyz.stignarnia.ui_base.utilities.extensions.navigateBack
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.ui_statistics_movies.databinding.FragmentStatisticsMoviesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
      statisticsMoviesToolbar.setOnClickListener { navigateBack() }
      statisticsMoviesRatings.onMovieClickListener = {
        openMovieDetails(it.movie.tmdbId)
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
