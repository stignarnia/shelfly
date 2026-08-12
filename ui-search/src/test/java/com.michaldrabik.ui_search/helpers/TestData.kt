package xyz.stignarnia.ui_search.helpers

import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_search.recycler.SearchListItem
import java.util.UUID

object TestData {

  val SEARCH_LIST_ITEM = SearchListItem(
    id = UUID.randomUUID(),
    show = Show.EMPTY,
    movie = Movie.EMPTY,
    image = Image.createUnknown(ImageType.POSTER),
    translation = null,
    order = 0,
    isFollowed = false,
    isLoading = false,
    isWatchlist = false,
    spoilers = SpoilersSettings.INITIAL,
  )
}
