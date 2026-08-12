package xyz.stignarnia.ui_discover_movies.helpers.itemtype

import xyz.stignarnia.ui_model.ImageType

internal interface ImageTypeProvider {
  fun getImageType(position: Int): ImageType
}
