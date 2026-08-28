package xyz.stignarnia.uiDiscover.helpers

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import xyz.stignarnia.common.Config.MAIN_GRID_SPAN
import xyz.stignarnia.common.Config.MAIN_GRID_SPAN_TABLET
import xyz.stignarnia.uiBase.utilities.extensions.isTablet

internal object DiscoverLayoutManagerProvider {
  fun provideLayoutManager(context: Context): GridLayoutManager {
    val span = if (context.isTablet()) MAIN_GRID_SPAN_TABLET else MAIN_GRID_SPAN
    return GridLayoutManager(context, span)
  }
}
