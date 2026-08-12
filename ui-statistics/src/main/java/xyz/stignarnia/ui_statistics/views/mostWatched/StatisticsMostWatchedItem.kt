package xyz.stignarnia.ui_statistics.views.mostWatched

import xyz.stignarnia.ui_base.common.ListItem
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.Translation

data class StatisticsMostWatchedItem(
  override val show: Show,
  val episodes: List<Episode>,
  val seasonsCount: Long,
  override val image: Image,
  override val isLoading: Boolean = false,
  val translation: Translation? = null,
) : ListItem
