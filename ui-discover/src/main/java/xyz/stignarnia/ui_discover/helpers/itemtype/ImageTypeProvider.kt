package xyz.stignarnia.ui_discover.helpers.itemtype

import xyz.stignarnia.ui_model.ImageType

internal interface ImageTypeProvider {

  fun getImageType(position: Int): ImageType
}
