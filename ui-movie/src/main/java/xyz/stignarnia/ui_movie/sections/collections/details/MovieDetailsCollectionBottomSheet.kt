package xyz.stignarnia.ui_movie.sections.collections.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.common.FastLinearLayoutManager
import xyz.stignarnia.ui_base.common.sheets.context_menu.ContextMenuBottomSheet
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.fadeOut
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.requireParcelable
import xyz.stignarnia.ui_base.utilities.extensions.screenHeight
import xyz.stignarnia.ui_base.utilities.extensions.showErrorSnackbar
import xyz.stignarnia.ui_base.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_movie.R
import xyz.stignarnia.ui_movie.databinding.ViewMovieCollectionDetailsBinding
import xyz.stignarnia.ui_movie.sections.collections.details.recycler.MovieDetailsCollectionAdapter
import xyz.stignarnia.ui_movie.sections.collections.details.recycler.MovieDetailsCollectionItem
import xyz.stignarnia.ui_navigation.java.NavigationArgs
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_COLLECTION_ID
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_DETAILS
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MovieDetailsCollectionBottomSheet : BaseBottomSheetFragment(R.layout.view_movie_collection_details) {

  companion object {
    const val SHOW_BACK_UP_BUTTON_THRESHOLD = 25

    fun createBundle(
      collectionId: IdTmdb,
      sourceMovieId: IdTmdb,
    ) = Bundle().apply {
      putParcelable(ARG_ID, collectionId)
      putParcelable(ARG_MOVIE_ID, sourceMovieId)
    }
  }

  private val viewModel by viewModels<MovieDetailsCollectionViewModel>()
  private val binding by viewBinding(ViewMovieCollectionDetailsBinding::bind)

  private val collectionId by lazy { requireParcelable<IdTmdb>(ARG_ID) }
  private val sourceMovieId by lazy { requireParcelable<IdTmdb>(ARG_MOVIE_ID) }

  private var adapter: MovieDetailsCollectionAdapter? = null
  private var layoutManager: LinearLayoutManager? = null

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    setupView()
    setupRecycler()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.messageFlow.collect { renderSnackbar(it) } },
      doAfterLaunch = {
        viewModel.loadCollection(collectionId)
      },
    )
  }

  private fun setupView() {
    with(binding) {
      val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
      with(behavior) {
        peekHeight = (screenHeight() * 0.55).toInt()
        skipCollapsed = true
        state = BottomSheetBehavior.STATE_COLLAPSED
      }
      backToTopButton.onClick {
        backToTopButton.fadeOut(150)
        itemsRecycler.smoothScrollToPosition(0)
      }
    }
  }

  private fun setupRecycler() {
    layoutManager = FastLinearLayoutManager(context, VERTICAL, false)
    adapter = MovieDetailsCollectionAdapter(
      onItemClickListener = ::openDetails,
      onItemLongClickListener = ::openContextDetails,
      onMissingImageListener = viewModel::loadMissingImage,
      onMissingTranslationListener = viewModel::loadMissingTranslation,
    )
    with(binding.itemsRecycler) {
      adapter = this@MovieDetailsCollectionBottomSheet.adapter
      layoutManager = this@MovieDetailsCollectionBottomSheet.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      removeOnScrollListener(recyclerScrollListener)
      addOnScrollListener(recyclerScrollListener)
    }
  }

  private fun openDetails(item: MovieDetailsCollectionItem) {
    if (item !is MovieDetailsCollectionItem.MovieItem) return
    if (item.movie.ids.tmdb == sourceMovieId) {
      dismiss()
      return
    }

    val resultBundle = Bundle().apply { putParcelable(ARG_COLLECTION_ID, collectionId) }
    setFragmentResult(REQUEST_DETAILS, resultBundle)

    val argsBundle = Bundle().apply { putLong(ARG_MOVIE_ID, item.movie.tmdbId) }
    requireParentFragment()
      .findNavController()
      .navigate(R.id.actionMovieCollectionDialogToMovie, argsBundle)
  }

  private fun openContextDetails(item: MovieDetailsCollectionItem) {
    if (item !is MovieDetailsCollectionItem.MovieItem) return
    if (item.movie.ids.tmdb == sourceMovieId) return

    setFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == NavigationArgs.REQUEST_ITEM_MENU) {
        viewModel.loadCollection(collectionId)
      }
      clearFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU)
    }

    val bundle = ContextMenuBottomSheet.createBundle(
      idTmdb = item.movie.ids.tmdb,
      detailsEnabled = false,
    )
    navigateTo(R.id.actionMovieCollectionDialogToContextDialog, bundle)
  }

  @SuppressLint("SetTextI18n")
  private fun render(uiState: MovieDetailsCollectionUiState) {
    uiState.run {
      items?.let { adapter?.setItems(it) }
    }
  }

  private fun renderSnackbar(message: MessageEvent) {
    when (message) {
      is MessageEvent.Info -> binding.rootLayout.showInfoSnackbar(getString(message.textRestId))
      is MessageEvent.Error -> binding.rootLayout.showErrorSnackbar(getString(message.textRestId))
    }
  }

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }

  private val recyclerScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(
      recyclerView: RecyclerView,
      newState: Int,
    ) {
      if (newState != SCROLL_STATE_IDLE) {
        return
      }
      if ((layoutManager?.findFirstVisibleItemPosition() ?: 0) >= SHOW_BACK_UP_BUTTON_THRESHOLD) {
        binding.backToTopButton.fadeIn(150)
      } else {
        binding.backToTopButton.fadeOut(150)
      }
    }
  }
}
