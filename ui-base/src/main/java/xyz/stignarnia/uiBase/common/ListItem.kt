package xyz.stignarnia.uiBase.common

import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Show

interface ListItem {
  val show: Show
  val image: Image
  val isLoading: Boolean

  infix fun isSameAs(other: ListItem) = show.ids.tmdb == other.show.ids.tmdb
}
