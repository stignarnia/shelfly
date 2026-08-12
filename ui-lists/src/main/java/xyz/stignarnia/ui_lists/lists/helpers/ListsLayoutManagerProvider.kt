package xyz.stignarnia.ui_lists.lists.helpers

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.ui_base.utilities.extensions.isTablet

internal object ListsLayoutManagerProvider {

  fun provideLayoutManger(
    context: Context,
    settings: SettingsViewModeRepository,
  ): RecyclerView.LayoutManager =
    if (context.isTablet()) {
      GridLayoutManager(context, settings.tabletGridSpanSize)
    } else {
      LinearLayoutManager(context, VERTICAL, false)
    }
}
