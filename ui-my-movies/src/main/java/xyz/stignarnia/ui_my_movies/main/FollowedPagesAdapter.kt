@file:Suppress("DEPRECATION")

package xyz.stignarnia.ui_my_movies.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import xyz.stignarnia.ui_my_movies.R
import xyz.stignarnia.ui_my_movies.hidden.HiddenFragment
import xyz.stignarnia.ui_my_movies.mymovies.MyMoviesFragment
import xyz.stignarnia.ui_my_movies.watchlist.WatchlistFragment

class FollowedPagesAdapter(
  fragManager: FragmentManager,
  private val context: Context,
) : FragmentStatePagerAdapter(fragManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

  companion object {
    const val PAGES_COUNT = 3
  }

  override fun getCount() = PAGES_COUNT

  override fun getItem(position: Int): Fragment =
    when (position) {
      0 -> MyMoviesFragment()
      1 -> WatchlistFragment()
      2 -> HiddenFragment()
      else -> throw IllegalStateException("Unknown position")
    }

  override fun getPageTitle(position: Int) =
    when (position) {
      0 -> context.getString(R.string.menuMyMovies)
      1 -> context.getString(R.string.menuWatchlistMovies)
      2 -> context.getString(R.string.menuHidden)
      else -> throw IllegalStateException()
    }
}
