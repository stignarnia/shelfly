package xyz.stignarnia.shelfly.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import xyz.stignarnia.shelfly.R
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.ui_widgets.BaseWidgetProvider.Companion.EXTRA_MOVIE_ID
import xyz.stignarnia.ui_widgets.BaseWidgetProvider.Companion.EXTRA_SHOW_ID
import xyz.stignarnia.ui_widgets.search.SearchWidgetProvider

abstract class BaseActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    // Everything before super, so the theme is final before a single resource is read against it.
    // Mutating it later left cached colour state lists from the previous configuration in place, which is what put black chips on a light page.
    // The night mode is already correct by now - the settings screen sets it before asking for the recreate - so super has no theme of its own to re-apply over these.
    ThemeApplier.applyNightMode(this)
    ThemeApplier.applyOverlays(this)
    super.onCreate(savedInstanceState)
  }

  private val actionKeys = arrayOf(
    EXTRA_SHOW_ID,
    EXTRA_MOVIE_ID,
  )

  protected fun findNavHostFragment() = supportFragmentManager.findFragmentById(R.id.navigationHost) as? NavHostFragment

  protected abstract fun handleSearchWidgetClick()

  fun handleNotification(
    intent: Intent?,
    action: () -> Unit = {},
  ) {
    val extras = intent?.extras ?: return
    if (extras.containsKey(SearchWidgetProvider.EXTRA_WIDGET_SEARCH_CLICK)) {
      intent.removeExtra(SearchWidgetProvider.EXTRA_WIDGET_SEARCH_CLICK)
      handleSearchWidgetClick()
      return
    }
    actionKeys.forEach {
      if (extras.containsKey(it)) {
        handleShowMovieExtra(intent, it, action)
      }
    }
  }

  private fun handleShowMovieExtra(
    intent: Intent,
    key: String,
    action: () -> Unit,
  ) {
    // Taken off the intent rather than off the extras, which are a copy: onCreate replays the stored intent after a recreation - a locale or a theme change - and the notification must not open a second time.
    val itemId = intent.getStringExtra(key)?.toLong() ?: -1
    intent.removeExtra(key)
    val bundle = Bundle().apply {
      putLong(ARG_SHOW_ID, itemId)
      putLong(ARG_MOVIE_ID, itemId)
    }

    findNavHostFragment()?.findNavController()?.run {
      try {
        val isShow = key == EXTRA_SHOW_ID
        if (isShow) {
          navigate(R.id.actionNavigateShowDetailsFragment, bundle)
        } else {
          navigate(R.id.actionNavigateMovieDetailsFragment, bundle)
        }
        action()
      } catch (error: Throwable) {
      }
    }
  }
}
