package xyz.stignarnia.ui_widgets.progress_movies

import android.content.Intent
import android.widget.RemoteViewsService
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_progress_movies.progress.cases.ProgressMoviesItemsCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProgressMoviesWidgetService : RemoteViewsService() {

  @Inject lateinit var progressLoadItemsCase: ProgressMoviesItemsCase
  @Inject lateinit var settingsRepository: SettingsRepository

  override fun onGetViewFactory(intent: Intent?) =
    ProgressMoviesWidgetViewsFactory(
      applicationContext,
      progressLoadItemsCase,
      settingsRepository,
    )
}
