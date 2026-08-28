package xyz.stignarnia.uiMyShows.common.recycler

import xyz.stignarnia.common.extensions.toZonedDateTime
import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiModel.UpcomingFilter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

sealed class CollectionListItem(
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean = false,
) : ListItem {
  fun getReleaseDate(): ZonedDateTime? = show.firstAired.toZonedDateTime()

  data class ShowItem(
    override val show: Show,
    override val image: Image,
    override val isLoading: Boolean = false,
    val dateFormat: DateTimeFormatter,
    val translation: Translation? = null,
    val userRating: Int? = null,
    val sortOrder: SortOrder? = null,
    val spoilers: Spoilers,
  ) : CollectionListItem(
      show = show,
      image = image,
      isLoading = isLoading,
    ) {
    data class Spoilers(
      val isSpoilerHidden: Boolean,
      val isSpoilerRatingsHidden: Boolean,
      val isSpoilerTapToReveal: Boolean,
    )
  }

  data class FiltersItem(
    val sortOrder: SortOrder,
    val sortType: SortType,
    val networks: List<String>,
    val genres: List<Genre>,
    val upcoming: UpcomingFilter,
    val count: Int,
  ) : CollectionListItem(
      show = Show.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    ) {
    fun hasActiveFilters() = upcoming.isActive() || networks.isNotEmpty() || genres.isNotEmpty()
  }
}
