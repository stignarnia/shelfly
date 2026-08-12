package xyz.stignarnia.shelfly.utilities.deeplink

import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Show

data class DeepLinkBundle(
  val show: Show? = null,
  val movie: Movie? = null,
) {

  companion object {
    val EMPTY = DeepLinkBundle()
  }
}
