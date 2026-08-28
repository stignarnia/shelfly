package xyz.stignarnia.uiLists.lists.helpers

import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Show

data class ListsItemImage(
  val image: Image,
  val show: Show? = null,
  val movie: Movie? = null,
) {
  fun getIds(): Ids? {
    if (show != null) return show.ids
    if (movie != null) return movie.ids
    return null
  }

  fun isShow() = show != null

  fun isMovie() = movie != null
}
