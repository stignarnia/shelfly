package xyz.stignarnia.uiMovie.sections.streamings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.addDivider
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiMovie.MovieDetailsFragment
import xyz.stignarnia.uiMovie.MovieDetailsViewModel
import xyz.stignarnia.uiMovie.R
import xyz.stignarnia.uiMovie.databinding.FragmentMovieDetailsStreamingsBinding
import xyz.stignarnia.uiStreamings.recycler.StreamingAdapter

@AndroidEntryPoint
class MovieDetailsStreamingsFragment :
  BaseFragment<MovieDetailsStreamingsViewModel>(
    R.layout.fragment_movie_details_streamings,
  ) {
  private val parentViewModel by viewModels<MovieDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<MovieDetailsStreamingsViewModel>()
  private val binding by viewBinding(FragmentMovieDetailsStreamingsBinding::bind)

  private var streamingAdapter: StreamingAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { parentViewModel.parentMovieState.collect { it?.let { viewModel.loadStreamings(it) } } },
      { viewModel.uiState.collect { render(it) } },
    )
  }

  private fun setupView() {
    streamingAdapter = StreamingAdapter()
    binding.movieDetailsStreamingsRecycler.apply {
      setHasFixedSize(true)
      adapter = streamingAdapter
      layoutManager = LinearLayoutManager(requireContext(), HORIZONTAL, false)
      addDivider(R.drawable.divider_horizontal_list, HORIZONTAL)
    }
  }

  private fun render(uiState: MovieDetailsStreamingsUiState) {
    with(uiState) {
      streamings?.let {
        if (streamingAdapter?.itemCount != 0) return@let
        val (items, isLocal) = it
        streamingAdapter?.setItems(items)
        if (items.isNotEmpty()) {
          (requireParentFragment() as MovieDetailsFragment).showStreamingsView(animate = !isLocal)
        }
      }
    }
  }

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    streamingAdapter = null
    super.onDestroyView()
  }
}
