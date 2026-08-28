package xyz.stignarnia.uiShow.sections.people

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.common.Mode
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.addDivider
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.trimWithSuffix
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiModel.Person.Department
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_PERSON
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_PERSON_ARGS
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_DETAILS
import xyz.stignarnia.uiPeople.details.PersonDetailsArgs
import xyz.stignarnia.uiPeople.details.PersonDetailsBottomSheet
import xyz.stignarnia.uiPeople.list.PeopleListBottomSheet
import xyz.stignarnia.uiShow.R
import xyz.stignarnia.uiShow.ShowDetailsEvent.OpenPeopleSheet
import xyz.stignarnia.uiShow.ShowDetailsEvent.OpenPersonSheet
import xyz.stignarnia.uiShow.ShowDetailsFragment
import xyz.stignarnia.uiShow.ShowDetailsViewModel
import xyz.stignarnia.uiShow.databinding.FragmentShowDetailsPeopleBinding
import xyz.stignarnia.uiShow.sections.people.recycler.ActorsAdapter

@AndroidEntryPoint
class ShowDetailsPeopleFragment : BaseFragment<ShowDetailsPeopleViewModel>(R.layout.fragment_show_details_people) {
  override val navigationId = R.id.showDetailsFragment
  private val binding by viewBinding(FragmentShowDetailsPeopleBinding::bind)

  private val parentViewModel by viewModels<ShowDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ShowDetailsPeopleViewModel>()

  private var actorsAdapter: ActorsAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { parentViewModel.parentShowState.collect { it?.let { viewModel.loadPeople(it) } } },
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
      doAfterLaunch = { viewModel.loadLastPerson() },
    )
  }

  private fun setupView() {
    actorsAdapter =
      ActorsAdapter().apply {
        itemClickListener = { viewModel.loadPersonDetails(it) }
      }
    with(binding) {
      showDetailsActorsLabel.text = getString(R.string.textPeople).replace(":", "")
      showDetailsActorsRecycler.apply {
        setHasFixedSize(true)
        adapter = actorsAdapter
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        addDivider(R.drawable.divider_horizontal_list, LinearLayoutManager.HORIZONTAL)
      }
    }
  }

  private fun openPersonSheet(
    show: Show,
    person: Person,
    personArgs: PersonDetailsArgs?,
  ) {
    handleSheetResult()
    val bundle = PersonDetailsBottomSheet.createBundle(person, show.ids.tmdb, personArgs)
    (requireParentFragment() as BaseFragment<*>)
      .navigateToSafe(R.id.actionShowDetailsFragmentToPerson, bundle)
  }

  private fun openPeopleSheet(event: OpenPeopleSheet) {
    val (show, people, department) = event

    if (people.isEmpty()) return
    if (people.size == 1) {
      viewModel.loadPersonDetails(people.first())
      return
    }

    handleSheetResult()

    val title =
      (requireParentFragment() as ShowDetailsFragment)
        .binding.showDetailsTitle.text
        .toString()
    val bundle = PeopleListBottomSheet.createBundle(show.ids.tmdb, title, Mode.SHOWS, department)
    navigateToSafe(R.id.actionShowDetailsFragmentToPeopleList, bundle)
  }

  private fun render(uiState: ShowDetailsPeopleUiState) {
    with(uiState) {
      actors?.let {
        if (actorsAdapter?.itemCount != 0) return@let
        actorsAdapter?.setItems(it)
        binding.showDetailsActorsRecycler.visibleIf(actors.isNotEmpty(), gone = false)
        binding.showDetailsActorsEmptyView.visibleIf(actors.isEmpty())
      }
      crew?.let { renderCrew(it) }
      isLoading.let {
        binding.showDetailsActorsProgress.visibleIf(it)
      }
    }
  }

  private fun renderCrew(crew: Map<Department, List<Person>>) {
    fun renderPeople(
      labelView: View,
      valueView: TextView,
      people: List<Person>,
      department: Department,
    ) {
      labelView.visibleIf(people.isNotEmpty())
      valueView.visibleIf(people.isNotEmpty())
      val limit = 3
      valueView.text =
        people
          .take(limit)
          .joinToString("\n") { it.name.trimWithSuffix(20, "…") }
          .plus(if (people.size > limit) "\n…" else "")
      valueView.onClick { viewModel.loadPeopleList(people, department) }
    }

    if (!crew.containsKey(Department.DIRECTING)) {
      return
    }

    val directors = crew[Department.DIRECTING] ?: emptyList()
    val writers = crew[Department.WRITING] ?: emptyList()
    val sound = crew[Department.SOUND] ?: emptyList()

    with(binding) {
      renderPeople(showDetailsDirectingLabel, showDetailsDirectingValue, directors, Department.DIRECTING)
      renderPeople(showDetailsWritingLabel, showDetailsWritingValue, writers, Department.WRITING)
      renderPeople(showDetailsMusicLabel, showDetailsMusicValue, sound, Department.SOUND)
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is OpenPersonSheet -> openPersonSheet(event.show, event.person, event.personArgs)
      is OpenPeopleSheet -> openPeopleSheet(event)
    }
  }

  private fun handleSheetResult() {
    requireParentFragment()
      .setFragmentResultListener(REQUEST_DETAILS) { _, bundle ->
        val person = BundleCompat.getParcelable(bundle, ARG_PERSON, Person::class.java)
        val personArgs = BundleCompat.getParcelable(bundle, ARG_PERSON_ARGS, PersonDetailsArgs::class.java)
        person?.let {
          viewModel.saveLastPerson(it, personArgs)
          bundle.clear()
        }
        requireParentFragment().clearFragmentResultListener(REQUEST_DETAILS)
      }
  }

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    actorsAdapter = null
    super.onDestroyView()
  }
}
