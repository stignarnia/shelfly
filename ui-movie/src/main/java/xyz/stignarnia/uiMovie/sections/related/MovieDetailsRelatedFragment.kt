package xyz.stignarnia.uiMovie.sections.related

import android.os.Bundle
import android.view.View
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.sheets.contextMenu.ContextMenuBottomSheet
import xyz.stignarnia.uiBase.utilities.extensions.addDivider
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiMovie.MovieDetailsViewModel
import xyz.stignarnia.uiMovie.R
import xyz.stignarnia.uiMovie.databinding.FragmentMovieDetailsRelatedBinding
import xyz.stignarnia.uiMovie.sections.related.recycler.RelatedListItem
import xyz.stignarnia.uiMovie.sections.related.recycler.RelatedMovieAdapter
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_ITEM_MENU

@AndroidEntryPoint
class MovieDetailsRelatedFragment :
  BaseFragment<MovieDetailsRelatedViewModel>(
    R.layout.fragment_movie_details_related,
  ) {
  override val navigationId = R.id.movieDetailsFragment
  private val binding by viewBinding(FragmentMovieDetailsRelatedBinding::bind)

  private val parentViewModel by viewModels<MovieDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<MovieDetailsRelatedViewModel>()

  private var relatedAdapter: RelatedMovieAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { parentViewModel.parentMovieState.collect { it?.let { viewModel.initRelatedMovies(it) } } },
      { viewModel.uiState.collect { render(it) } },
    )
  }

  private fun setupView() {
    relatedAdapter =
      RelatedMovieAdapter(
        itemClickListener = ::openDetails,
        itemLongClickListener = ::openContextMenu,
        missingImageListener = { ids, force -> viewModel.loadMissingImage(ids, force) },
      )
    binding.movieDetailsRelatedRecycler.apply {
      setHasFixedSize(true)
      adapter = relatedAdapter
      layoutManager = LinearLayoutManager(requireContext(), HORIZONTAL, false)
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      addDivider(R.drawable.divider_horizontal_list, HORIZONTAL)
    }
  }

  private fun openDetails(item: RelatedListItem) {
    val bundle = Bundle().apply { putLong(ARG_MOVIE_ID, item.movie.tmdbId) }
    navigateToSafe(R.id.actionMovieDetailsFragmentToSelf, bundle)
  }

  private fun openContextMenu(item: RelatedListItem) {
    requireParentFragment()
      .setFragmentResultListener(REQUEST_ITEM_MENU) { requestKey, _ ->
        if (requestKey == REQUEST_ITEM_MENU) {
          viewModel.loadRelatedMovies()
        }
        requireParentFragment().clearFragmentResultListener(REQUEST_ITEM_MENU)
      }

    val bundle = ContextMenuBottomSheet.createBundle(item.movie.ids.tmdb)
    navigateToSafe(R.id.actionMovieDetailsFragmentToContext, bundle)
  }

  private fun render(uiState: MovieDetailsRelatedUiState) {
    with(uiState) {
      with(binding) {
        relatedMovies?.let {
          relatedAdapter?.setItems(it)
          movieDetailsRelatedRecycler.visibleIf(it.isNotEmpty())
          movieDetailsRelatedLabel.fadeIf(it.isNotEmpty(), hardware = true)
        }
        isLoading.let {
          movieDetailsRelatedProgress.visibleIf(it)
        }
      }
    }
  }

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    relatedAdapter = null
    super.onDestroyView()
  }
}
