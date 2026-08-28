package xyz.stignarnia.uiProgress.history.entities

import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.HistoryPeriod
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiProgress.helpers.TranslationsBundle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import xyz.stignarnia.uiModel.Episode as EpisodeUi

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
