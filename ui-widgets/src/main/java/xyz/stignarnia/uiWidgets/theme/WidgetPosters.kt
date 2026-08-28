package xyz.stignarnia.uiWidgets.theme

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Fetches a list's posters together rather than one row at a time.
 *
 * A widget carrying its rows inside itself has no way to load an image lazily: every poster in the list has to be in hand before the widget can be sent at all.
 * Fetched one after another that is minutes on a cold cache, and the widget sits on the host's error view for all of it - which is exactly what it did.
 *
 * So they are fetched at [CONCURRENCY] at a time, each given [TIMEOUT_SECONDS] of its own.
 * A poster that does not arrive in time is left out and the row keeps its placeholder; the next update picks it up, by which point Glide has it cached.
 */
object WidgetPosters {
  private const val CONCURRENCY = 8
  private const val TIMEOUT_SECONDS = 4L

  /** What a poster of this size costs the widget's bitmap budget, whether or not it has arrived yet. */
  fun costOf(
    width: Int,
    height: Int,
  ) = width.toLong() * height * 4

  suspend fun fetch(
    context: Context,
    width: Int,
    height: Int,
    corner: Int,
    urls: Map<Long, String>,
  ): Map<Long, Bitmap> =
    coroutineScope {
      val gate = Semaphore(CONCURRENCY)
      urls
        .map { (id, url) ->
          async(Dispatchers.IO) {
            gate.withPermit {
              val bitmap =
                runCatching {
                  withContext(Dispatchers.IO) {
                    Glide
                      .with(context)
                      .asBitmap()
                      .load(url)
                      .transform(CenterCrop(), RoundedCorners(corner))
                      .submit(width, height)
                      .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  }
                }.getOrNull()
              id to bitmap
            }
          }
        }.awaitAll()
        .mapNotNull { (id, bitmap) -> bitmap?.let { id to it } }
        .toMap()
    }
}
