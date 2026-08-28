package xyz.stignarnia.uiSearch.helpers

import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiSearch.recycler.SearchListItem
import java.util.UUID

object TestData {
  val SEARCH_LIST_ITEM =
    SearchListItem(
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
