package xyz.stignarnia.uiPeople.details

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.common.FastLinearLayoutManager
import xyz.stignarnia.uiBase.utilities.TipsHost
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.fadeOut
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.extensions.screenHeight
import xyz.stignarnia.uiBase.utilities.extensions.showErrorSnackbar
import xyz.stignarnia.uiBase.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiModel.Tip
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_PERSON
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_PERSON_ARGS
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_DETAILS
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.ViewPersonDetailsBinding
import xyz.stignarnia.uiPeople.details.PersonDetailsUiEvent.ScrollToPosition
import xyz.stignarnia.uiPeople.details.links.PersonLinksBottomSheet
import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsAdapter
import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsItem
import xyz.stignarnia.uiPeople.gallery.PersonGalleryFragment

@AndroidEntryPoint
class PersonDetailsBottomSheet : BaseBottomSheetFragment(R.layout.view_person_details) {
  companion object {
    const val SHOW_BACK_UP_BUTTON_THRESHOLD = 25

    fun createBundle(
      person: Person,
      sourceId: IdTmdb,
      personArgs: PersonDetailsArgs?,
    ): Bundle =
      Bundle().apply {
        putParcelable(ARG_PERSON, person)
        putParcelable(ARG_PERSON_ARGS, personArgs ?: PersonDetailsArgs())
        putParcelable(ARG_ID, sourceId)
      }
  }

  private val viewModel by viewModels<PersonDetailsViewModel>()
  private val binding by viewBinding(ViewPersonDetailsBinding::bind)

  private val personArgs by lazy { requireParcelable<PersonDetailsArgs>(ARG_PERSON_ARGS) }
  private val person by lazy { requireParcelable<Person>(ARG_PERSON) }
  private val sourceId by lazy { requireParcelable<IdTmdb>(ARG_ID) }

  private var adapter: PersonDetailsAdapter? = null
  private var layoutManager: LinearLayoutManager? = null

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    setupView()
    setupTips()
    setupRecycler()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
      { viewModel.messageFlow.collect { renderSnackbar(it) } },
      doAfterLaunch = { viewModel.loadDetails(person, personArgs) },
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
      personDetailsRecyclerFab.onClick {
        personDetailsRecyclerFab.fadeOut(150)
        personDetailsRecycler.smoothScrollToPosition(0)
      }
    }
  }

  private fun setupTips() {
    val isShown = (requireActivity() as TipsHost).isTipShown(Tip.PERSON_DETAILS_GALLERY)
    if (!isShown && !person.imagePath.isNullOrBlank()) {
      val message = getString(Tip.PERSON_DETAILS_GALLERY.textResId)
      binding.personDetailsSnackHost.showInfoSnackbar(message, length = Snackbar.LENGTH_INDEFINITE) {
        (requireActivity() as TipsHost).setTipShow(Tip.PERSON_DETAILS_GALLERY)
      }
    }
  }

  private fun setupRecycler() {
    layoutManager = FastLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    adapter =
      PersonDetailsAdapter(
        onItemClickListener = { openDetails(it) },
        onLinksClickListener = { openLinksSheet(it) },
        onImageClickListener = { openGallery() },
        onImageMissingListener = { item, force -> viewModel.loadMissingImage(item, force) },
        onTranslationMissingListener = { item -> viewModel.loadMissingTranslation(item) },
        onFiltersChangeListener = { filters ->
          viewModel.loadCredits(
            person = person,
            filters = filters,
          )
        },
      )
    with(binding.personDetailsRecycler) {
      adapter = this@PersonDetailsBottomSheet.adapter
      layoutManager = this@PersonDetailsBottomSheet.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      removeOnScrollListener(recyclerScrollListener)
      addOnScrollListener(recyclerScrollListener)
    }
  }

  private fun openDetails(item: PersonDetailsItem) {
    val personBundle =
      Bundle().apply {
        putParcelable(ARG_PERSON, person)
        putParcelable(
          ARG_PERSON_ARGS,
          PersonDetailsArgs(
            isExpanded = isSheetExpanded(),
            isUpButtonVisible = binding.personDetailsRecyclerFab.isVisible,
            firstVisibleItemPosition = (layoutManager?.findLastVisibleItemPosition() ?: 0),
          ),
        )
      }
    if (item is PersonDetailsItem.CreditsShowItem && item.show.tmdbId != sourceId.id) {
      setFragmentResult(REQUEST_DETAILS, personBundle)
      val bundle = Bundle().apply { putLong(NavigationArgs.ARG_SHOW_ID, item.show.tmdbId) }
      requireParentFragment()
        .findNavController()
        .navigate(R.id.actionPersonDetailsDialogToShow, bundle)
    }
    if (item is PersonDetailsItem.CreditsMovieItem && item.movie.tmdbId != sourceId.id) {
      setFragmentResult(REQUEST_DETAILS, personBundle)
      val bundle = Bundle().apply { putLong(NavigationArgs.ARG_MOVIE_ID, item.movie.tmdbId) }
      requireParentFragment()
        .findNavController()
        .navigate(R.id.actionPersonDetailsDialogToMovie, bundle)
    }
  }

  private fun openGallery() {
    val personBundle = Bundle().apply { putParcelable(ARG_PERSON, person) }
    setFragmentResult(REQUEST_DETAILS, personBundle)
    val options = PersonGalleryFragment.createBundle(person)
    requireParentFragment()
      .findNavController()
      .navigate(R.id.actionPersonDetailsDialogToGallery, options)
  }

  private fun openLinksSheet(it: Person) {
    val options = PersonLinksBottomSheet.createBundle(it)
    navigateTo(R.id.actionPersonDetailsDialogToLinks, options)
  }

  private fun render(uiState: PersonDetailsUiState) {
    uiState.run {
      personDetailsItems?.let { adapter?.setItems(it) }
    }
  }

  private fun renderSnackbar(message: MessageEvent) {
    when (message) {
      is MessageEvent.Info -> binding.viewPersonDetailsRoot.showInfoSnackbar(getString(message.textRestId))
      is MessageEvent.Error -> binding.viewPersonDetailsRoot.showErrorSnackbar(getString(message.textRestId))
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is ScrollToPosition -> {
        if (event.isSheetExpanded) expandSheet()
        with(binding) {
          if (event.isUpButtonVisible) personDetailsRecyclerFab.fadeIn(150)
          personDetailsRecycler.postDelayed({
            personDetailsRecycler.scrollToPosition(event.position)
          }, 100)
        }
      }
    }
  }

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }

  private val recyclerScrollListener =
    object : RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(
        recyclerView: RecyclerView,
        newState: Int,
      ) {
        if (newState != RecyclerView.SCROLL_STATE_IDLE) return
        if ((layoutManager?.findFirstVisibleItemPosition() ?: 0) >= SHOW_BACK_UP_BUTTON_THRESHOLD) {
          binding.personDetailsRecyclerFab.fadeIn(150)
        } else {
          binding.personDetailsRecyclerFab.fadeOut(150)
        }
      }
    }
}
