package xyz.stignarnia.uiProgress.calendar.recycler

import androidx.annotation.StringRes
import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiProgress.helpers.TranslationsBundle
import java.time.format.DateTimeFormatter
import xyz.stignarnia.uiModel.Episode as EpisodeModel

sealed class CalendarListItem(
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean = false,
) : ListItem {
  data class Episode(
    override val show: Show,
    override val image: Image,
    override val isLoading: Boolean = false,
    val episode: EpisodeModel,
    val season: Season,
    val isWatched: Boolean,
    val isWatchlist: Boolean,
    val isSpoilerHidden: Boolean,
    val translations: TranslationsBundle? = null,
    val dateFormat: DateTimeFormatter? = null,
    val spoilers: SpoilersSettings? = null,
  ) : CalendarListItem(show, image, isLoading) {
    override fun isSameAs(other: ListItem) = episode.ids.tmdb == (other as? Episode)?.episode?.ids?.tmdb
  }

  data class Header(
    @get:StringRes val textResId: Int,
    val calendarMode: CalendarMode,
  ) : CalendarListItem(
      show = Show.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    ) {
    companion object {
      fun create(
        @StringRes textResId: Int,
        mode: CalendarMode,
      ) = Header(
        textResId = textResId,
        calendarMode = mode,
      )
    }

    override fun isSameAs(other: ListItem) = textResId == (other as? Header)?.textResId
  }

  data class Filters(
    val mode: CalendarMode,
    val premieres: Boolean,
  ) : CalendarListItem(
      show = Show.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    )
}
