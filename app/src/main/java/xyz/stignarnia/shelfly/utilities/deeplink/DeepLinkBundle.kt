package xyz.stignarnia.shelfly.utilities.deeplink

import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Show

data class DeepLinkBundle(
  val show: Show? = null,
  val movie: Movie? = null,
) {
  companion object {
    val EMPTY = DeepLinkBundle()
  }
}
