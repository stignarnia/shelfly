package xyz.stignarnia.ui_progress.history.entities

import xyz.stignarnia.ui_base.common.ListItem
import xyz.stignarnia.ui_model.HistoryPeriod
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_progress.helpers.TranslationsBundle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import xyz.stignarnia.ui_model.Episode as EpisodeUi

internal sealed class HistoryListItem(
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean = false,
) : ListItem {

  data class Episode(
    override val show: Show,
    override val image: Image,
    override val isLoading: Boolean = false,
    val episode: EpisodeUi,
    val season: Season,
    val translations: TranslationsBundle? = null,
    val dateFormat: DateTimeFormatter? = null,
  ) : HistoryListItem(show, image, isLoading) {

    override fun isSameAs(other: ListItem): Boolean = episode.ids.tmdb == (other as? Episode)?.episode?.ids?.tmdb
  }

  data class Header(
    val date: LocalDateTime,
    val language: String,
  ) : HistoryListItem(
      show = Show.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    ) {
    override fun isSameAs(other: ListItem): Boolean {
      val otherHeader = (other as? Header) ?: return false
      return date.isEqual(otherHeader.date)
    }
  }

  data class Filters(
    val period: HistoryPeriod,
  ) : HistoryListItem(
      show = Show.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    ) {
    override fun isSameAs(other: ListItem): Boolean = period == (other as? Filters)?.period
  }
}
