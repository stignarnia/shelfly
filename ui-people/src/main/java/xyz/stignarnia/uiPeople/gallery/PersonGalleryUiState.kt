package xyz.stignarnia.uiPeople.gallery

import xyz.stignarnia.uiModel.Image

data class PersonGalleryUiState(
  val images: List<Image>? = null,
  val isLoading: Boolean = false,
)
