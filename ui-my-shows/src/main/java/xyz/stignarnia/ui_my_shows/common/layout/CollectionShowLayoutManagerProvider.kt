package xyz.stignarnia.ui_my_shows.common.layout

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.common.ListViewMode.LIST_NORMAL
import xyz.stignarnia.ui_base.utilities.extensions.isTablet

internal object CollectionShowLayoutManagerProvider {

  fun provideLayoutManger(
    context: Context,
    viewMode: ListViewMode,
    gridSpanSize: Int,
  ): RecyclerView.LayoutManager =
    if (context.isTablet()) {
      provideTabletLayout(context, viewMode, gridSpanSize)
    } else {
      providePhoneLayout(context, viewMode)
    }

  private fun providePhoneLayout(
    context: Context,
    viewMode: ListViewMode,
  ): RecyclerView.LayoutManager =
    when (viewMode) {
      LIST_NORMAL -> LinearLayoutManager(context, VERTICAL, false)
    }

  private fun provideTabletLayout(
    context: Context,
    viewMode: ListViewMode,
    gridSpanSize: Int,
  ): RecyclerView.LayoutManager =
    when (viewMode) {
      LIST_NORMAL -> GridLayoutManager(context, gridSpanSize)
    }
}
