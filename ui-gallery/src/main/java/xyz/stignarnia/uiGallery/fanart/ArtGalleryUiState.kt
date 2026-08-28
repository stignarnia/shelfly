package xyz.stignarnia.uiGallery.fanart

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType

data class ArtGalleryUiState(
  val images: List<Image>? = null,
  val type: ImageType = ImageType.FANART,
  val pickedImage: Event<Image>? = null,
  val isLoading: Boolean = false,
)
