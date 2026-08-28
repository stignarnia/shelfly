package xyz.stignarnia.uiPeople.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.common.Mode
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.common.FastLinearLayoutManager
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.requireLong
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.requireString
import xyz.stignarnia.uiBase.utilities.extensions.screenHeight
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_DEPARTMENT
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_PERSON
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_TITLE
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_TYPE
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_DETAILS
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.ViewPeopleListBinding
import xyz.stignarnia.uiPeople.details.PersonDetailsBottomSheet
import xyz.stignarnia.uiPeople.list.recycler.PeopleListAdapter

@AndroidEntryPoint
class PeopleListBottomSheet : BaseBottomSheetFragment(R.layout.view_people_list) {
  companion object {
    fun createBundle(
      mediaIdTmdb: IdTmdb,
      mediaTitle: String,
      mode: Mode,
      department: Person.Department,
    ) = Bundle().apply {
      putLong(ARG_ID, mediaIdTmdb.id)
      putString(ARG_TITLE, mediaTitle)
      putSerializable(ARG_TYPE, mode.type)
      putSerializable(ARG_DEPARTMENT, department)
    }
  }

  private val viewModel by viewModels<PeopleListViewModel>()
  private val binding by viewBinding(ViewPeopleListBinding::bind)

  private val mediaIdTmdb by lazy { IdTmdb(requireLong(ARG_ID)) }
  private val mediaTitle by lazy { requireString(ARG_TITLE) }
  private val mode by lazy { Mode.fromType(requireString(ARG_TYPE)) }
  private val department by lazy { requireSerializable<Person.Department>(ARG_DEPARTMENT) }

  private var adapter: PeopleListAdapter? = null
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
      doAfterLaunch = {
        viewModel.loadPeople(
          mediaIdTmdb,
          mediaTitle,
          mode,
          department,
        )
      },
    )
  }

  private fun setupView() {
    val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
    with(behavior) {
      peekHeight = (screenHeight() * 0.45).toInt()
      skipCollapsed = true
      state = BottomSheetBehavior.STATE_COLLAPSED
    }
  }

  private fun setupRecycler() {
    layoutManager = FastLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    adapter =
      PeopleListAdapter(
        onItemClickListener = { openDetails(it) },
      )
    with(binding.viewPeopleListRecycler) {
      adapter = this@PeopleListBottomSheet.adapter
      layoutManager = this@PeopleListBottomSheet.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }
  }

  private fun openDetails(item: Person) {
    setFragmentResult(REQUEST_DETAILS, Bundle().apply { putParcelable(ARG_PERSON, item) })
    val bundle = PersonDetailsBottomSheet.createBundle(item, mediaIdTmdb, null)
    findNavController().navigate(R.id.actionPeopleListDialogToDetails, bundle)
  }

  private fun render(uiState: PeopleListUiState) {
    uiState.run {
      peopleItems?.let { adapter?.setItems(it) }
    }
  }

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
