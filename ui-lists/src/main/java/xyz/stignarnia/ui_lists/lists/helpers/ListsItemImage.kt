package xyz.stignarnia.ui_lists.lists.helpers

import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Show

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
