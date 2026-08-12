package xyz.stignarnia.ui_discover_movies.helpers.itemtype

import xyz.stignarnia.ui_model.ImageType

private const val BUFFER = 14

internal class PhoneImageTypeProvider : ImageTypeProvider {

  override fun getImageType(position: Int): ImageType {
    if (position % BUFFER == 0) return ImageType.FANART_WIDE
    if ((position + (BUFFER - 5)) % BUFFER == 0) return ImageType.FANART
    if ((position + (BUFFER - 9)) % BUFFER == 0) return ImageType.FANART
    return ImageType.POSTER
  }
}
