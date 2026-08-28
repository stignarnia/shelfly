package xyz.stignarnia.uiProgress.history

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.OnScrollResetListener
import xyz.stignarnia.uiBase.common.OnSearchClickListener
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.HistoryPeriod
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.databinding.FragmentHistoryBinding
import xyz.stignarnia.uiProgress.helpers.ProgressLayoutManagerProvider
import xyz.stignarnia.uiProgress.history.entities.HistoryListItem
import xyz.stignarnia.uiProgress.history.filters.HistoryPeriodFilterBottomSheet
import xyz.stignarnia.uiProgress.history.filters.HistoryPeriodFilterBottomSheet.Companion.ARG_SELECTED_FILTER
import xyz.stignarnia.uiProgress.history.filters.HistoryPeriodFilterBottomSheet.Companion.REQUEST_KEY
import xyz.stignarnia.uiProgress.history.recycler.HistoryAdapter
import xyz.stignarnia.uiProgress.main.ProgressMainFragment
import xyz.stignarnia.uiProgress.main.ProgressMainViewModel
import javax.inject.Inject

@AndroidEntryPoint
internal class HistoryFragment :
  BaseFragment<HistoryViewModel>(R.layout.fragment_history),
  OnSearchClickListener,
  OnScrollResetListener {
  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.progressMainFragment
  private val binding by viewBinding(FragmentHistoryBinding::bind)

  override val viewModel by viewModels<HistoryViewModel>()
  private val parentViewModel by viewModels<ProgressMainViewModel>({ requireParentFragment() })

  private var adapter: HistoryAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var isSearching = false

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupRecycler()
    setupInsets()

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        with(parentViewModel) {
          launch { uiState.collect { viewModel.handleParentAction(it) } }
        }
        with(viewModel) {
          launch { uiState.collect { render(it) } }
        }
      }
    }
  }

  private fun setupRecycler() {
    val gridSpanSize = settings.tabletGridSpanSize
    layoutManager = ProgressLayoutManagerProvider.provideLayoutManger(requireContext(), gridSpanSize)
    (layoutManager as? GridLayoutManager)?.run {
      withSpanSizeLookup { position ->
        when (adapter?.getItems()?.get(position)) {
          is HistoryListItem.Header -> gridSpanSize
          is HistoryListItem.Filters -> gridSpanSize
          is HistoryListItem.Episode -> 1
          else -> throw IllegalStateException()
        }
      }
    }
    adapter =
      HistoryAdapter(
        onItemClick = { requireMainFragment().openShowDetails(it.show) },
        onDetailsClick = {
          requireMainFragment().openEpisodeDetails(
            show = it.show,
            episode = it.episode,
            season = it.season,
          )
        },
        onDatesFilterClick = { openPeriodFilterDialog(it) },
        onImageMissing = { item, force -> viewModel.findMissingImage(item, force) },
        onTranslationMissing = { viewModel.findMissingTranslation(it) },
        listChangeListener = {
          requireMainFragment().resetTranslations()
          layoutManager?.scrollToPosition(0)
        },
      )
    binding.recycler.apply {
      adapter = this@HistoryFragment.adapter
      layoutManager = this@HistoryFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupInsets() {
    val recyclerPadding =
      if (moviesEnabled) {
        R.dimen.progressHistoryTabsViewPadding
      } else {
        R.dimen.progressHistoryTabsViewPaddingNoModes
      }

    binding.recycler.doOnApplyWindowInsets { view, insets, padding, _ ->
      val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
      val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(
        top = systemInsets.top + tabletOffset + dimenToPx(recyclerPadding),
        bottom = systemInsets.bottom + padding.bottom,
      )
    }
  }

  override fun onEnterSearch() {
    isSearching = true

    with(binding) {
      recycler.translationY = dimenToPx(R.dimen.progressSearchLocalOffset).toFloat()
      recycler.smoothScrollToPosition(0)
    }
  }

  override fun onExitSearch() {
    isSearching = false
    with(binding) {
      recycler.translationY = 0F
      recycler.smoothScrollToPosition(0)
    }
  }

  private fun render(uiState: HistoryUiState) {
    with(uiState) {
      with(binding) {
        adapter?.setItems(
          newItems = items,
          notifyChange = resetScrollEvent?.consume() == true,
        )
        if (isLoading) {
          recycler.gone()
        } else {
          recycler.fadeIn(150, withHardware = true)
        }
        emptyView.root.visibleIf(!items.any { it is HistoryListItem.Episode } && !isLoading && !isSearching)
        progressBar.visibleIf(isLoading && !isSearching)
      }
    }
  }

  private fun openPeriodFilterDialog(filter: HistoryPeriod) {
    val args = HistoryPeriodFilterBottomSheet.createBundle(filter)
    requireParentFragment().setFragmentResultListener(REQUEST_KEY) { _, bundle ->
      val selected = bundle.requireSerializable<HistoryPeriod>(ARG_SELECTED_FILTER)
      viewModel.setPeriod(selected)
    }
    navigateToSafe(R.id.actionProgressFragmentToDatesFilter, args)
  }

  override fun onScrollReset() = binding.recycler.smoothScrollToPosition(0)

  private fun requireMainFragment() = requireParentFragment() as ProgressMainFragment

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }

  override fun setupBackPressed() = Unit
}
