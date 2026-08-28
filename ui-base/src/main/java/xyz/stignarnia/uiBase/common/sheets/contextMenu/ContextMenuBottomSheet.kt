package xyz.stignarnia.uiBase.common.sheets.contextMenu

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import xyz.stignarnia.common.Config
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.databinding.ViewContextMenuBinding
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.extensions.showErrorSnackbar
import xyz.stignarnia.uiBase.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.extensions.withFailListener
import xyz.stignarnia.uiBase.utilities.extensions.withSuccessListener
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageStatus
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_OPTIONS
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_ITEM_MENU

abstract class ContextMenuBottomSheet : BaseBottomSheetFragment(R.layout.view_context_menu) {
  companion object {
    private const val ARG_SHOW_PIN_BUTTONS = "ARG_SHOW_PIN_BUTTONS"
    private const val ARG_DETAILS_ENABLED = "ARG_DETAILS_ENABLED"

    fun createBundle(
      idTmdb: IdTmdb,
      showPinButtons: Boolean = false,
      detailsEnabled: Boolean = true,
    ) = Bundle().apply {
      putParcelable(ARG_ID, idTmdb)
      putBundle(
        ARG_OPTIONS,
        Bundle().apply {
          putBoolean(ARG_SHOW_PIN_BUTTONS, showPinButtons)
          putBoolean(ARG_DETAILS_ENABLED, detailsEnabled)
        },
      )
    }
  }

  protected val binding by viewBinding(ViewContextMenuBinding::bind)

  protected val itemId by lazy { requireParcelable<IdTmdb>(ARG_ID) }
  private val showPinButtons by lazy { requireParcelable<Bundle>(ARG_OPTIONS).getBoolean(ARG_SHOW_PIN_BUTTONS) }
  private val detailsEnabled by lazy { requireParcelable<Bundle>(ARG_OPTIONS).getBoolean(ARG_DETAILS_ENABLED) }

  private val cornerRadius by lazy { dimenToPx(R.dimen.mediaTileCorner).toFloat() }
  private val cornerBigRadius by lazy { dimenToPx(R.dimen.collectionItemCorner).toFloat() }
  private val centerCropTransformation by lazy { CenterCrop() }
  private val cornersTransformation by lazy {
    GranularRoundedCorners(cornerBigRadius, cornerRadius, cornerRadius, cornerRadius)
  }

  protected val colorAccent by lazy { ContextCompat.getColor(requireContext(), R.color.colorAccent) }
  protected val colorGray by lazy { ContextCompat.getColor(requireContext(), R.color.colorGrayLight) }

  protected abstract fun openDetails()

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  protected open fun setupView() {
    with(binding) {
      contextMenuItemDescription.setInitialLines(5)
      contextMenuItemPinButtonsLayout.visibleIf(showPinButtons)
      contextMenuItemSeparator2.visibleIf(showPinButtons)
      contextMenuItemImage.onClick { if (detailsEnabled) openDetails() }
      contextMenuItemPlaceholder.onClick { if (detailsEnabled) openDetails() }
    }
  }

  protected fun renderImage(image: Image) {
    Glide.with(this).clear(binding.contextMenuItemImage)

    if (image.status == ImageStatus.UNAVAILABLE) {
      binding.contextMenuItemPlaceholder.visible()
      binding.contextMenuItemImage.gone()
      return
    }

    Glide
      .with(this)
      .load(image.fullFileUrl)
      .transform(centerCropTransformation, cornersTransformation)
      .transition(DrawableTransitionOptions.withCrossFade(Config.IMAGE_FADE_DURATION_MS))
      .withSuccessListener {
        binding.contextMenuItemPlaceholder.gone()
        binding.contextMenuItemImage.visible()
      }.withFailListener {
        binding.contextMenuItemPlaceholder.visible()
        binding.contextMenuItemImage.gone()
      }.into(binding.contextMenuItemImage)
  }

  protected fun renderSnackbar(message: MessageEvent) {
    when (message) {
      is MessageEvent.Info -> binding.contextMenuItemSnackbarHost.showInfoSnackbar(getString(message.textRestId))
      is MessageEvent.Error -> binding.contextMenuItemSnackbarHost.showErrorSnackbar(getString(message.textRestId))
    }
  }

  protected fun close() {
    setFragmentResult(REQUEST_ITEM_MENU, Bundle.EMPTY)
    closeSheet()
    dismiss()
  }
}
