package xyz.stignarnia.uiProgress.main.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.calendar.CalendarFragment
import xyz.stignarnia.uiProgress.history.HistoryFragment
import xyz.stignarnia.uiProgress.progress.ProgressFragment

class ProgressMainAdapter(
  fragManager: FragmentManager,
  lifecycle: Lifecycle,
  private val context: Context,
) : FragmentStateAdapter(fragManager, lifecycle) {
  companion object {
    const val PAGES_COUNT = 3
  }

  override fun createFragment(position: Int): Fragment =
    when (position) {
      0 -> ProgressFragment()
      1 -> CalendarFragment()
      2 -> HistoryFragment()
      else -> throw IllegalStateException("Unknown position")
    }

  override fun getItemCount() = PAGES_COUNT

  /**
   * ViewPager2 has no page titles of its own; TabLayoutMediator asks for them when it binds a tab.
   */
  fun getPageTitle(position: Int): String =
    when (position) {
      0 -> context.getString(R.string.tabProgress)
      1 -> context.getString(R.string.tabCalendar)
      2 -> context.getString(R.string.tabHistory)
      else -> throw IllegalStateException("Unknown position")
    }
}
