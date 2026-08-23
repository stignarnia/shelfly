package xyz.stignarnia.ui_widgets.theme

import android.content.Context
import android.os.Parcel
import android.view.WindowManager
import android.widget.RemoteViews

/**
 * Builds a widget's rows into the RemoteViews itself, within the budgets the framework enforces on them.
 *
 * A collection bound the old way - a service the launcher calls back into for each row - is never delivered to the host: see [WidgetPalettes] and BaseWidgetProvider.
 * Carrying the rows inline is what makes the whole widget deliverable in one go, frame and list together.
 *
 * The cost is that every row travels at once, so there are two ceilings to respect, and going over either is not a truncation but a TransactionTooLargeException that drops the host's connection and stops every widget on the device until it comes back:
 *
 * - Bitmaps: the service allows 6 x the display's pixels, and hands out 90% of that.
 * - Everything else: 800,000 bytes, being 80% of a binder transaction.
 *
 * Rows are measured as they are added and the list stops when either would be spent, so a long collection is cut deliberately rather than thrown.
 * Both budgets are then cut again by [SAFETY], because a row measured on its own parcel is not the identical cost of that row inside the collection - the framework leaves itself the same kind of slack for the same reason.
 */
object WidgetCollection {

  /**
   * How many rows are worth building at all.
   *
   * The budgets below are what the framework will accept; this is what is sensible to spend getting there.
   * Every row's poster is fetched before the widget can be sent - inline rows have no way to be lazy - so the list's length is paid in blocking image loads inside a broadcast, and a few hundred of those is minutes.
   * A widget nobody has scrolled a hundred rows into does not need the hundred and first.
   */
  private const val MAX_ROWS = 100

  /** The framework's own cap on the non bitmap half, from RemoteViews.MAX_SINGLE_PARCEL_SIZE. */
  private const val PARCEL_BUDGET = 800_000

  /** Bitmap memory is 6 x the display's pixels in AppWidgetServiceImpl, of which AppWidgetManager spends 90%. */
  private const val BITMAP_PIXEL_FACTOR = 6

  /**
   * What is left after measurement error.
   * Measuring a row alone misses what the collection adds around it, and being under costs a row while being over costs the home screen.
   */
  private const val SAFETY = 0.7

  /**
   * Fills [builder] from [rows] until a budget is spent, and says how many were taken.
   *
   * [rows] is asked for one row at a time so nothing beyond the budget is ever built: a row that would not fit is the last one loaded, not one of many already in memory.
   */
  fun fill(
    context: Context,
    count: Int,
    viewTypeCount: Int,
    moreRow: () -> RemoteViews,
    rows: (Int) -> Pair<Long, RemoteViews>,
  ): Pair<RemoteViews.RemoteCollectionItems, Int> {
    val builder = RemoteViews.RemoteCollectionItems
      .Builder()
      .setHasStableIds(true)
      .setViewTypeCount(viewTypeCount)

    var budget = (bitmapBudget(context) * SAFETY).toLong()
    var structure = (PARCEL_BUDGET * SAFETY).toLong()
    var taken = 0

    val parcel = Parcel.obtain()
    try {
      for (position in 0 until minOf(count, MAX_ROWS)) {
        val (id, views) = rows(position)

        parcel.setDataPosition(0)
        parcel.setDataSize(0)
        views.writeToParcel(parcel, 0)
        val size = parcel.dataSize().toLong()

        // The measured size carries the row's bitmaps with it, which is the half that runs out first; what is left over is charged to the parcel.
        if (size > budget || size > structure) break

        budget -= size
        structure -= ROW_STRUCTURE_ESTIMATE
        if (structure <= 0) break

        builder.addItem(id, views)
        taken++
      }
    } finally {
      parcel.recycle()
    }

    // Say so rather than stopping silently: a list that just ends looks like a list that has nothing more in it.
    if (taken < count) {
      builder.addItem(MORE_ID, moreRow())
    }

    return builder.build() to taken
  }

  /** The id of the row that stands for everything that did not fit; far outside anything a show or film would use. */
  private const val MORE_ID = Long.MAX_VALUE

  /** What a row costs the parcel once its bitmaps are counted elsewhere: text, ids and flags, an order of magnitude under a kilobyte. */
  private const val ROW_STRUCTURE_ESTIMATE = 1_500L

  private fun bitmapBudget(context: Context): Long {
    val bounds = context
      .getSystemService(WindowManager::class.java)
      .maximumWindowMetrics
      .bounds
    return BITMAP_PIXEL_FACTOR.toLong() * bounds.width() * bounds.height() * 9 / 10
  }
}
