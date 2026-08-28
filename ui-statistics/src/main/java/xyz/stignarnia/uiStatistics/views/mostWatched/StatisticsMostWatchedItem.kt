package xyz.stignarnia.uiStatistics.views.mostWatched

import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.Translation

data class StatisticsMostWatchedItem(
  override val show: Show,
  val episodes: List<Episode>,
  val seasonsCount: Long,
  override val image: Image,
  override val isLoading: Boolean = false,
  val translation: Translation? = null,
) : ListItem
