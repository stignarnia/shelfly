package xyz.stignarnia.ui_show.sections.related

import android.os.Bundle
import android.view.View
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.SimpleItemAnimator
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.common.sheets.context_menu.ContextMenuBottomSheet
import xyz.stignarnia.ui_base.utilities.extensions.addDivider
import xyz.stignarnia.ui_base.utilities.extensions.fadeIf
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.navigateToSafe
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_ITEM_MENU
import xyz.stignarnia.ui_show.R
import xyz.stignarnia.ui_show.ShowDetailsViewModel
import xyz.stignarnia.ui_show.databinding.FragmentShowDetailsRelatedBinding
import xyz.stignarnia.ui_show.sections.related.recycler.RelatedListItem
import xyz.stignarnia.ui_show.sections.related.recycler.RelatedShowAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowDetailsRelatedFragment : BaseFragment<ShowDetailsRelatedViewModel>(R.layout.fragment_show_details_related) {

  override val navigationId = R.id.showDetailsFragment
  private val binding by viewBinding(FragmentShowDetailsRelatedBinding::bind)

  private val parentViewModel by viewModels<ShowDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ShowDetailsRelatedViewModel>()

  private var relatedAdapter: RelatedShowAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { parentViewModel.parentShowState.collect { it?.let { viewModel.initRelatedShows(it) } } },
      { viewModel.uiState.collect { render(it) } },
    )
  }

  private fun setupView() {
    relatedAdapter = RelatedShowAdapter(
      itemClickListener = ::openDetails,
      itemLongClickListener = ::openContextMenu,
      missingImageListener = viewModel::loadMissingImage,
    )
    binding.showDetailsRelatedRecycler.apply {
      setHasFixedSize(true)
      adapter = relatedAdapter
      layoutManager = LinearLayoutManager(requireContext(), HORIZONTAL, false)
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      addDivider(R.drawable.divider_horizontal_list, HORIZONTAL)
    }
  }

  private fun openDetails(item: RelatedListItem) {
    val bundle = Bundle().apply { putLong(ARG_SHOW_ID, item.show.tmdbId) }
    navigateToSafe(R.id.actionShowDetailsFragmentToSelf, bundle)
  }

  private fun openContextMenu(item: RelatedListItem) {
    requireParentFragment()
      .setFragmentResultListener(REQUEST_ITEM_MENU) { requestKey, _ ->
        if (requestKey == REQUEST_ITEM_MENU) {
          viewModel.loadRelatedShows()
        }
        requireParentFragment().clearFragmentResultListener(REQUEST_ITEM_MENU)
      }

    val bundle = ContextMenuBottomSheet.createBundle(item.show.ids.tmdb)
    navigateToSafe(R.id.actionShowDetailsFragmentToContext, bundle)
  }

  private fun render(uiState: ShowDetailsRelatedUiState) {
    with(uiState) {
      with(binding) {
        relatedShows?.let {
          relatedAdapter?.setItems(it)
          showDetailsRelatedRecycler.visibleIf(it.isNotEmpty())
          showDetailsRelatedLabel.fadeIf(it.isNotEmpty(), hardware = true)
        }
        isLoading.let {
          showDetailsRelatedProgress.visibleIf(it)
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
