package xyz.stignarnia.uiMyShows.myshows.recycler

import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType.POSTER
import xyz.stignarnia.uiModel.MyShowsSection
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.Translation

data class MyShowsItem(
  val type: Type,
  val header: Header?,
  val recentsSection: RecentsSection?,
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean,
  val spoilers: Spoilers,
  val translation: Translation? = null,
  val userRating: Int? = null,
  val sortOrder: SortOrder? = null,
) : ListItem {
  enum class Type {
    RECENT_SHOWS,
    ALL_SHOWS_HEADER,
    ALL_SHOWS_ITEM,
  }

  data class Header(
    val section: MyShowsSection,
    val itemCount: Int,
    val sortOrder: Pair<SortOrder, SortType>?,
    val networks: List<String>?,
    val genres: List<Genre>?,
  )

  data class RecentsSection(
    val items: List<MyShowsItem>,
  )

  data class Spoilers(
    val isSpoilerHidden: Boolean,
    val isSpoilerRatingsHidden: Boolean,
    val isSpoilerTapToReveal: Boolean,
  )

  companion object {
    fun createHeader(
      section: MyShowsSection,
      itemCount: Int,
      sortOrder: Pair<SortOrder, SortType>?,
      networks: List<String>?,
      genres: List<Genre>?,
    ) = MyShowsItem(
      type = Type.ALL_SHOWS_HEADER,
      header = Header(section, itemCount, sortOrder, networks, genres),
      recentsSection = null,
      show = Show.EMPTY,
      image = Image.createUnavailable(POSTER),
      isLoading = false,
      spoilers =
        Spoilers(
          isSpoilerHidden = false,
          isSpoilerRatingsHidden = false,
          isSpoilerTapToReveal = false,
        ),
    )

    fun createRecentsSection(shows: List<MyShowsItem>) =
      MyShowsItem(
        type = Type.RECENT_SHOWS,
        header = null,
        recentsSection = RecentsSection(shows),
        show = Show.EMPTY,
        image = Image.createUnavailable(POSTER),
        isLoading = false,
        spoilers =
          Spoilers(
            isSpoilerHidden = false,
            isSpoilerRatingsHidden = false,
            isSpoilerTapToReveal = false,
          ),
      )
  }
}
