package xyz.stignarnia.uiGallery.fanart

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.navigateBack
import xyz.stignarnia.uiBase.utilities.extensions.nextPage
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.openWebUrl
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.updateTopMargin
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiGallery.R
import xyz.stignarnia.uiGallery.databinding.FragmentArtGalleryBinding
import xyz.stignarnia.uiGallery.fanart.recycler.ArtGalleryAdapter
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.ImageFamily
import xyz.stignarnia.uiModel.ImageFamily.SHOW
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.ImageType.POSTER
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_FAMILY
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_TYPE

@AndroidEntryPoint
class ArtGalleryFragment : BaseFragment<ArtGalleryViewModel>(R.layout.fragment_art_gallery) {
  override val viewModel by viewModels<ArtGalleryViewModel>()
  private val binding by viewBinding(FragmentArtGalleryBinding::bind)

  private val showId by lazy { IdTmdb(arguments?.getLong(ARG_SHOW_ID, -1) ?: -1) }
  private val movieId by lazy { IdTmdb(arguments?.getLong(ARG_MOVIE_ID, -1) ?: -1) }
  private val family by lazy { requireSerializable<ImageFamily>(ARG_FAMILY) }
  private val type by lazy { requireSerializable<ImageType>(ARG_TYPE) }

  private var galleryAdapter: ArtGalleryAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupInsets()

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        with(viewModel) {
          launch { uiState.collect { render(it) } }
          val id = if (family == SHOW) showId else movieId
          loadImages(id, family, type)
        }
      }
    }
  }

  override fun onDestroyView() {
    galleryAdapter = null
    super.onDestroyView()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    with(binding) {
      when (newConfig.orientation) {
        ORIENTATION_LANDSCAPE -> {
          artGalleryPagerIndicatorWhite.visible()
          artGalleryPagerIndicator.gone()
          artGalleryPagerIndicatorWhite.setViewPager(artGalleryPager)
        }

        ORIENTATION_PORTRAIT -> {
          artGalleryPagerIndicatorWhite.gone()
          artGalleryPagerIndicator.visible()
          artGalleryPagerIndicator.setViewPager(artGalleryPager)
        }

        else -> {
          Timber.d("Unused orientation")
        }
      }
    }
  }

  private fun setupView() {
    with(binding) {
      artGalleryBackArrow.onClick {
        navigateBack()
      }
      artGalleryBrowserIcon.onClick {
        val currentIndex = artGalleryPager.currentItem
        val image = galleryAdapter?.getItem(currentIndex)
        image?.fullFileUrl?.let { openWebUrl(it) }
      }
      galleryAdapter =
        ArtGalleryAdapter(
          onItemClickListener = { artGalleryPager.nextPage() },
        )
      artGalleryPager.run {
        adapter = galleryAdapter
        offscreenPageLimit = 2
        artGalleryPagerIndicator.setViewPager(this)
        adapter?.registerAdapterDataObserver(artGalleryPagerIndicator.adapterDataObserver)
      }
    }
  }

  private fun setupInsets() {
    requireView().doOnApplyWindowInsets { view, insets, _, _ ->
      val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(bottom = inset.bottom)
      with(binding) {
        artGalleryBackArrow.updateTopMargin(inset.top)
        artGalleryBrowserIcon.updateTopMargin(inset.top)
        artGalleryPagerIndicator.updateLayoutParams<MarginLayoutParams> {
          updateMargins(bottom = inset.bottom + dimenToPx(R.dimen.spaceNormal))
        }
        artGalleryPagerIndicatorWhite.updateLayoutParams<MarginLayoutParams> {
          updateMargins(bottom = inset.bottom + dimenToPx(R.dimen.spaceNormal))
        }
        artGalleryImagesProgress.updateLayoutParams<MarginLayoutParams> {
          updateMargins(bottom = inset.bottom + dimenToPx(R.dimen.spaceNormal))
        }
      }
    }
  }

  private fun render(uiState: ArtGalleryUiState) {
    uiState.run {
      with(binding) {
        images?.let {
          galleryAdapter?.setItems(it, type)
          artGalleryEmptyView.visibleIf(it.isEmpty())
          artGalleryBrowserIcon.visibleIf(it.isNotEmpty())
        }
        artGalleryImagesProgress.visibleIf(isLoading)
      }
    }
  }

  override fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      isEnabled = false
      findNavControl()?.popBackStack()
    }
  }
}
