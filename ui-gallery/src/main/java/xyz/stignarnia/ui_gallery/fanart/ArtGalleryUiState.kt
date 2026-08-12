package xyz.stignarnia.ui_gallery.fanart

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType

data class ArtGalleryUiState(
  val images: List<Image>? = null,
  val type: ImageType = ImageType.FANART,
  val pickedImage: Event<Image>? = null,
  val isLoading: Boolean = false,
)
