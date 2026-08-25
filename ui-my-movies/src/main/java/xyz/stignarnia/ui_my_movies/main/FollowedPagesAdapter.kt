package xyz.stignarnia.ui_my_movies.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import xyz.stignarnia.ui_my_movies.R
import xyz.stignarnia.ui_my_movies.hidden.HiddenFragment
import xyz.stignarnia.ui_my_movies.mymovies.MyMoviesFragment
import xyz.stignarnia.ui_my_movies.watchlist.WatchlistFragment

class FollowedPagesAdapter(
  fragManager: FragmentManager,
  lifecycle: Lifecycle,
  private val context: Context,
) : FragmentStateAdapter(fragManager, lifecycle) {

  companion object {
    const val PAGES_COUNT = 3
  }

  override fun getItemCount() = PAGES_COUNT

  override fun createFragment(position: Int): Fragment =
    when (position) {
      0 -> MyMoviesFragment()
      1 -> WatchlistFragment()
      2 -> HiddenFragment()
      else -> throw IllegalStateException("Unknown position")
    }

  /**
   * ViewPager2 has no page titles of its own; TabLayoutMediator asks for them when it binds a tab.
   */
  fun getPageTitle(position: Int): String =
    when (position) {
      0 -> context.getString(R.string.menuMyMovies)
      1 -> context.getString(R.string.menuWatchlistMovies)
      2 -> context.getString(R.string.menuHidden)
      else -> throw IllegalStateException()
    }
}
