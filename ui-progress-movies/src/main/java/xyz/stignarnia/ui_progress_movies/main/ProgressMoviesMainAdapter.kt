package xyz.stignarnia.ui_progress_movies.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import xyz.stignarnia.ui_progress_movies.R
import xyz.stignarnia.ui_progress_movies.calendar.CalendarMoviesFragment
import xyz.stignarnia.ui_progress_movies.progress.ProgressMoviesFragment

class ProgressMoviesMainAdapter(
  fragManager: FragmentManager,
  lifecycle: Lifecycle,
  private val context: Context,
) : FragmentStateAdapter(fragManager, lifecycle) {

  companion object {
    const val PAGES_COUNT = 2
  }

  override fun getItemCount() = PAGES_COUNT

  override fun createFragment(position: Int): Fragment =
    when (position) {
      0 -> ProgressMoviesFragment()
      1 -> CalendarMoviesFragment()
      else -> throw IllegalStateException("Unknown position")
    }

  /**
   * ViewPager2 has no page titles of its own; TabLayoutMediator asks for them when it binds a tab.
   */
  fun getPageTitle(position: Int): String =
    when (position) {
      0 -> context.getString(R.string.tabMoviesProgress)
      1 -> context.getString(R.string.tabMoviesCalendar)
      else -> throw IllegalStateException()
    }
}
