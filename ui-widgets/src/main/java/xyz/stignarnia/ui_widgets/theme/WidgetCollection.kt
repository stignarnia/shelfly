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
 * How much of the second a row costs is measured; how much of the first is [posterCost] for that row, given rather than measured - and zero for a row that carries no poster at all, such as a date header.
 * That is deliberate: rows are counted on a pass that carries no posters yet - see [fill] - and a poster's cost is known from its dimensions without having it in hand.
 *
 * Neither budget is discounted again here. Both already carry the framework's own slack - 80% of a transaction, 90% of the bitmap memory - and cutting twice only makes the list shorter than the platform itself would allow.
 */
object WidgetCollection {

  /**
   * The framework's own cap on the non bitmap half, from RemoteViews.MAX_SINGLE_PARCEL_SIZE.
   * Already 80% of a binder transaction, the remaining fifth being the slack they keep for what measuring a row alone does not see - so it is spent whole here rather than discounted twice.
   */
  private const val PARCEL_BUDGET = 800_000L

  /** Bitmap memory is 6 x the display's pixels in AppWidgetServiceImpl, of which AppWidgetManager spends 90%. */
  private const val BITMAP_PIXEL_FACTOR = 6

  /** The id of the row that stands for everything that did not fit; far outside anything a show or film would use. */
  private const val MORE_ID = Long.MAX_VALUE

  /**
   * Decides how many of [count] rows fit, and builds them.
   *
   * [rows] is expected to return rows without their posters: this is the pass that decides the length of the list, and it must not wait on the network to do it.
   * Feed the same [taken] back through [build] once the posters are in hand.
   */
  fun fill(
    context: Context,
    count: Int,
    viewTypeCount: Int,
    posterCost: (Int) -> Long,
    moreRow: () -> RemoteViews,
    rows: (Int) -> Pair<Long, RemoteViews>,
  ): Pair<RemoteViews.RemoteCollectionItems, Int> {
    var bitmaps = bitmapBudget(context)
    var structure = PARCEL_BUDGET
    var taken = 0

    val built = mutableListOf<Pair<Long, RemoteViews>>()
    val parcel = Parcel.obtain()
    try {
      for (position in 0 until count) {
        val row = rows(position)

        parcel.setDataPosition(0)
        parcel.setDataSize(0)
        row.second.writeToParcel(parcel, 0)

        val poster = posterCost(position)
        if (parcel.dataSize() > structure || poster > bitmaps) break

        structure -= parcel.dataSize()
        bitmaps -= poster
        built += row
        taken++
      }
    } finally {
      parcel.recycle()
    }

    return build(built, count, viewTypeCount, moreRow) to taken
  }

  /** Assembles rows that have already been counted, so the second pass costs no measurement. */
  fun build(
    rows: List<Pair<Long, RemoteViews>>,
    count: Int,
    viewTypeCount: Int,
    moreRow: () -> RemoteViews,
  ): RemoteViews.RemoteCollectionItems {
    val builder = RemoteViews.RemoteCollectionItems
      .Builder()
      .setHasStableIds(true)
      .setViewTypeCount(viewTypeCount)

    rows.forEach { (id, views) -> builder.addItem(id, views) }

    // Say so rather than stopping silently: a list that just ends looks like a list that has nothing more in it.
    if (rows.size < count) {
      builder.addItem(MORE_ID, moreRow())
    }

    return builder.build()
  }

  private fun bitmapBudget(context: Context): Long {
    val bounds = context
      .getSystemService(WindowManager::class.java)
      .maximumWindowMetrics
      .bounds
    return BITMAP_PIXEL_FACTOR.toLong() * bounds.width() * bounds.height() * 9 / 10
  }
}
