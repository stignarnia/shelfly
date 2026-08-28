package xyz.stignarnia.uiMovie.sections.collections.list

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.repository.movies.MovieCollectionsRepository.Source
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.addDivider
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.MovieCollection
import xyz.stignarnia.uiMovie.MovieDetailsEvent.OpenCollectionSheet
import xyz.stignarnia.uiMovie.MovieDetailsFragment
import xyz.stignarnia.uiMovie.MovieDetailsViewModel
import xyz.stignarnia.uiMovie.R
import xyz.stignarnia.uiMovie.databinding.FragmentMovieDetailsCollectionBinding
import xyz.stignarnia.uiMovie.sections.collections.details.MovieDetailsCollectionBottomSheet
import xyz.stignarnia.uiMovie.sections.collections.list.recycler.MovieCollectionAdapter
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_COLLECTION_ID

@AndroidEntryPoint
class MovieDetailsCollectionsFragment :
  BaseFragment<MovieDetailsCollectionsViewModel>(
    R.layout.fragment_movie_details_collection,
  ) {
  override val navigationId = R.id.movieDetailsFragment

  private val parentViewModel by viewModels<MovieDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<MovieDetailsCollectionsViewModel>()

  private val binding by viewBinding(FragmentMovieDetailsCollectionBinding::bind)

  private var collectionsAdapter: MovieCollectionAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { parentViewModel.parentMovieState.collect { it?.let { viewModel.loadCollections(it) } } },
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
      doAfterLaunch = { viewModel.loadLastOpenedCollection() },
    )
  }

  private fun setupView() {
    collectionsAdapter =
      MovieCollectionAdapter(
        itemClickListener = { viewModel.loadCollection(it) },
      )
    binding.movieDetailsCollectionRecycler.apply {
      setHasFixedSize(true)
      adapter = collectionsAdapter
      layoutManager = LinearLayoutManager(requireContext(), HORIZONTAL, false)
      addDivider(R.drawable.divider_horizontal_list, HORIZONTAL)
    }
  }

  private fun render(uiState: MovieDetailsCollectionsUiState) {
    with(uiState) {
      collections?.let { (collections, source) ->
        collectionsAdapter?.setItems(collections)
        if (collections.isNotEmpty()) {
          (requireParentFragment() as MovieDetailsFragment).showCollectionsView(animate = source == Source.REMOTE)
        }
      }
      isLoading.let {
        binding.movieDetailsCollectionProgress.visibleIf(it)
      }
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is OpenCollectionSheet -> openCollectionDetails(event.movie, event.collection)
    }
  }

  private fun openCollectionDetails(
    movie: Movie,
    collection: MovieCollection,
  ) {
    requireParentFragment()
      .setFragmentResultListener(NavigationArgs.REQUEST_DETAILS) { _, bundle ->
        BundleCompat.getParcelable(bundle, ARG_COLLECTION_ID, IdTmdb::class.java)?.let {
          viewModel.saveLastOpenedCollection(it)
        }
      }
    val bundle =
      MovieDetailsCollectionBottomSheet.createBundle(
        collectionId = collection.id,
        sourceMovieId = movie.ids.tmdb,
      )
    navigateToSafe(R.id.actionMovieDetailsFragmentToCollection, bundle)
  }

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    collectionsAdapter = null
    super.onDestroyView()
  }
}
