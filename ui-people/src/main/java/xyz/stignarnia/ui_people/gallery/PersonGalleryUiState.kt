package xyz.stignarnia.ui_people.gallery

import xyz.stignarnia.ui_model.Image

data class PersonGalleryUiState(
  val images: List<Image>? = null,
  val isLoading: Boolean = false,
)
