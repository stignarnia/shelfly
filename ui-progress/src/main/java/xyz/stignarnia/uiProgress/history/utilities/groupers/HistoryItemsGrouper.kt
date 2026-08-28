package xyz.stignarnia.uiProgress.history.utilities.groupers

import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.uiProgress.history.entities.HistoryListItem
import xyz.stignarnia.uiProgress.history.entities.HistoryListItem.Episode
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

internal class HistoryItemsGrouper
  @Inject
  constructor() {
    fun groupByDay(
      items: List<Episode>,
      language: String,
    ): List<HistoryListItem> {
      val itemsMap =
        items
          .groupBy {
            it.episode.lastWatchedAt!!
              .toLocalZone()
              .truncatedTo(DAYS)
          }.toSortedMap(compareByDescending { it.toMillis() })

      return itemsMap.entries.fold(mutableListOf()) { acc, entry ->
        acc.apply {
          if (entry.value.isNotEmpty()) {
            add(
              HistoryListItem.Header(
                date = entry.key.toLocalDateTime(),
                language = language,
              ),
            )
            addAll(
              entry.value.sortedWith(
                compareByDescending<Episode> { it.episode.lastWatchedAt?.toMillis() }
                  .thenByDescending { it.episode.season }
                  .thenByDescending { it.episode.number },
              ),
            )
          }
        }
      }
    }
  }
