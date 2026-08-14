package xyz.stignarnia.ui_base.common

import androidx.recyclerview.widget.RecyclerView
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter

/**
 * Restricts the overscroll effect to the top of a list.
 *
 * Reporting the end as never reached leaves the bottom to the platform's own
 * edge effect, so only the pull-down gesture is decorated.
 */
class OverscrollTopAdapter(
  recycler: RecyclerView,
) : RecyclerViewOverScrollDecorAdapter(recycler) {
  override fun isInAbsoluteEnd() = false
}
