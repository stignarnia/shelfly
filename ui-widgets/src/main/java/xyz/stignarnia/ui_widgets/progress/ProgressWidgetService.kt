package xyz.stignarnia.ui_widgets.progress

import android.content.Intent
import android.widget.RemoteViewsService
import xyz.stignarnia.ui_progress.progress.cases.ProgressItemsCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProgressWidgetService : RemoteViewsService() {

  @Inject lateinit var progressItemsCase: ProgressItemsCase

  override fun onGetViewFactory(intent: Intent?) =
    ProgressWidgetViewsFactory(
      applicationContext,
      progressItemsCase,
    )
}
