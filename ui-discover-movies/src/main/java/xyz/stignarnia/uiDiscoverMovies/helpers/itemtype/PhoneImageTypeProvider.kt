package xyz.stignarnia.uiDiscoverMovies.helpers.itemtype

import xyz.stignarnia.uiModel.ImageType

private const val BUFFER = 14

internal class PhoneImageTypeProvider : ImageTypeProvider {
  override fun getImageType(position: Int): ImageType {
    if (position % BUFFER == 0) return ImageType.FANART_WIDE
    if ((position + (BUFFER - 5)) % BUFFER == 0) return ImageType.FANART
    if ((position + (BUFFER - 9)) % BUFFER == 0) return ImageType.FANART
    return ImageType.POSTER
  }
}
