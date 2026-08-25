package xyz.stignarnia.ui_lists.manage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.SimpleItemAnimator
import xyz.stignarnia.common.Mode
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.events.Event
import xyz.stignarnia.ui_base.events.EventsManager
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.requireLong
import xyz.stignarnia.ui_base.utilities.extensions.requireString
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_lists.R
import xyz.stignarnia.ui_lists.databinding.ViewManageListsBinding
import xyz.stignarnia.ui_lists.manage.helpers.ManageListsDividerDecoration
import xyz.stignarnia.ui_lists.manage.recycler.ManageListsAdapter
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_TYPE
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_CREATE_LIST
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_MANAGE_LISTS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ManageListsBottomSheet : BaseBottomSheetFragment(R.layout.view_manage_lists) {

  private val viewModel by viewModels<ManageListsViewModel>()
  private val binding by viewBinding(ViewManageListsBinding::bind)

  private val itemId by lazy { IdTmdb(requireLong(ARG_ID)) }
  private val itemType by lazy { requireString(ARG_TYPE) }

  private var adapter: ManageListsAdapter? = null
  private var layoutManager: LinearLayoutManager? = null

  @Inject lateinit var eventsManager: EventsManager

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupRecycler()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { eventsManager.events.collect { handleEvent(it) } },
      doAfterLaunch = { viewModel.loadLists(itemId, itemType) },
    )
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        with(viewModel) {
          launch { uiState.collect { render(it) } }
          loadLists(itemId, itemType)
        }
      }
    }
  }

  private fun setupRecycler() {
    layoutManager = LinearLayoutManager(context, VERTICAL, false)
    adapter = ManageListsAdapter(
      itemCheckListener = { item, isChecked ->
        viewModel.onListItemChecked(itemId, itemType, item, isChecked)
      },
    )
    binding.viewManageListsRecycler.apply {
      adapter = this@ManageListsBottomSheet.adapter
      layoutManager = this@ManageListsBottomSheet.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      addItemDecoration(ManageListsDividerDecoration(requireContext()))
    }
  }

  private fun setupView() {
    with(binding) {
      viewManageListsButton.onClick { closeSheet() }
      viewManageListsCreateButton.onClick {
        setFragmentResultListener(REQUEST_CREATE_LIST) { _, _ -> viewModel.loadLists(itemId, itemType) }
        navigateTo(R.id.actionManageListsDialogToCreateListDialog, Bundle.EMPTY)
      }
      if (itemType == Mode.MOVIES.type) {
        viewManageListsSubtitle.setText(R.string.textManageListsMovies)
      }
    }
  }

  private fun render(uiState: ManageListsUiState) {
    uiState.run {
      items?.let {
        adapter?.setItems(it)
        binding.viewManageListsEmptyView.layoutManageListsEmpty.visibleIf(it.isEmpty())
      }
    }
  }

  private fun handleEvent(event: Event) = Unit

  override fun onDestroyView() {
    setFragmentResult(REQUEST_MANAGE_LISTS, Bundle())
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
