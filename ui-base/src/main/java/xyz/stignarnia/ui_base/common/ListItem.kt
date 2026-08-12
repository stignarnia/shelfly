package xyz.stignarnia.ui_base.common

import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Show

interface ListItem {
  val show: Show
  val image: Image
  val isLoading: Boolean

  infix fun isSameAs(other: ListItem) = show.ids.tmdb == other.show.ids.tmdb
}
