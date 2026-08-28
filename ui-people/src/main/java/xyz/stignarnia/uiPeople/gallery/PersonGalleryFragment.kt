package xyz.stignarnia.uiPeople.gallery

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateBack
import xyz.stignarnia.uiBase.utilities.extensions.nextPage
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.openWebUrl
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.extensions.updateTopMargin
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.FragmentPersonGalleryBinding
import xyz.stignarnia.uiPeople.gallery.recycler.PersonGalleryAdapter

@AndroidEntryPoint
class PersonGalleryFragment : BaseFragment<PersonGalleryViewModel>(R.layout.fragment_person_gallery) {
  companion object {
    fun createBundle(person: Person): Bundle = Bundle().apply { putParcelable(ARG_ID, person.ids.tmdb) }
  }

  override val viewModel by viewModels<PersonGalleryViewModel>()
  private val binding by viewBinding(FragmentPersonGalleryBinding::bind)

  private val personId by lazy { requireParcelable<IdTmdb>(ARG_ID) }

  private var galleryAdapter: PersonGalleryAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupStatusBar()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      doAfterLaunch = { viewModel.loadImages(personId) },
    )
  }

  override fun onDestroyView() {
    galleryAdapter = null
    super.onDestroyView()
  }

  private fun setupView() {
    with(binding) {
      personGalleryBackArrow.onClick {
        navigateBack()
      }
      personGalleryBrowserIcon.onClick {
        val currentIndex = personGalleryPager.currentItem
        val image = galleryAdapter?.getItem(currentIndex)
        openImageInBrowser(image?.fullFileUrl)
      }
      galleryAdapter =
        PersonGalleryAdapter(
          onItemClickListener = { personGalleryPager.nextPage() },
        )
      personGalleryPager.run {
        adapter = galleryAdapter
        offscreenPageLimit = 2
        personGalleryPagerIndicator.setViewPager(this)
        adapter?.registerAdapterDataObserver(personGalleryPagerIndicator.adapterDataObserver)
      }
    }
  }

  private fun setupStatusBar() {
    requireView().doOnApplyWindowInsets { _, insets, _, _ ->
      val margin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
      with(binding) {
        personGalleryBackArrow.updateTopMargin(margin)
        personGalleryBrowserIcon.updateTopMargin(margin)
      }
    }
  }

  private fun render(uiState: PersonGalleryUiState) {
    uiState.run {
      with(binding) {
        images?.let {
          galleryAdapter?.setItems(it)
          personGalleryEmptyView.visibleIf(it.isEmpty())
          personGalleryBrowserIcon.visibleIf(it.isNotEmpty())
        }
        isLoading.let {
          personGalleryImagesProgress.visibleIf(it)
        }
      }
    }
  }

  private fun openImageInBrowser(url: String?) {
    url?.let { requireContext().openWebUrl(it) }
  }

  override fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      isEnabled = false
      findNavControl()?.popBackStack()
    }
  }
}
