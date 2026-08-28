package xyz.stignarnia.uiDiscoverMovies.helpers.itemtype

import xyz.stignarnia.uiModel.ImageType

internal interface ImageTypeProvider {
  fun getImageType(position: Int): ImageType
}
