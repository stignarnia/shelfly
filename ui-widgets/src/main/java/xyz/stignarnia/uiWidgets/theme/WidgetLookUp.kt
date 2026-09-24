package xyz.stignarnia.uiWidgets.theme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Looks up what the rows a widget sent are missing from the cache, which the rows themselves only ever read.
 * After a language switch that is every poster and every translation, and without this a widget stays without them until the app happens to open the same list.
 *
 * Whatever is found cannot join the rows already sent, whose number was decided without it - see WidgetCollection.fill - so the widget then has to be built again.
 * Runs within the same limits as [WidgetPosters].
 */
object WidgetLookUp {
  /** Whether [find] found something for any of [items]. */
  suspend fun <T> any(
    items: List<T>,
    find: suspend (T) -> Boolean,
  ): Boolean =
    coroutineScope {
      val gate = Semaphore(WidgetPosters.CONCURRENCY)
      items
        .map { item ->
          async(Dispatchers.IO) {
            gate.withPermit {
              runCatching { withTimeout(TimeUnit.SECONDS.toMillis(WidgetPosters.TIMEOUT_SECONDS)) { find(item) } }.getOrDefault(false)
            }
          }
        }.awaitAll()
        .any { it }
    }
}
