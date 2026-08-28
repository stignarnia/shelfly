package xyz.stignarnia.uiSearch.recycler

import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import java.util.UUID

data class SearchListItem(
  val id: UUID,
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean = false,
  val movie: Movie,
  val order: Int,
  val isFollowed: Boolean = false,
  val isWatchlist: Boolean = false,
  val translation: Translation? = null,
  val spoilers: SpoilersSettings,
) : ListItem {
  val isShow = show != Show.EMPTY
  val isMovie = movie != Movie.EMPTY

  val votes = if (isShow) show.votes else movie.votes
  val title = if (isShow) show.title else movie.title
  val overview = if (isShow) show.overview else movie.overview
  val year = if (isShow) show.year else movie.year
  val network = if (isShow) show.network else ""

  override fun isSameAs(other: ListItem) = (id == (other as SearchListItem).id)
}
