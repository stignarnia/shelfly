package xyz.stignarnia.ui_widgets.progress_movies

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import xyz.stignarnia.ui_base.common.WidgetsProvider
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_progress_movies.main.cases.ProgressMoviesMainCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ProgressMoviesWidgetCheckService :
  JobIntentService(),
  CoroutineScope {

  companion object {
    private const val JOB_ID = 1010
    private const val EXTRA_MOVIE_ID = "EXTRA_MOVIE_ID"

    fun initialize(
      context: Context,
      movieId: IdTmdb,
    ) {
      val intent = Intent().apply {
        putExtra(EXTRA_MOVIE_ID, movieId.id)
      }
      enqueueWork(
        context,
        ProgressMoviesWidgetCheckService::class.java,
        JOB_ID,
        intent,
      )
    }
  }

  override val coroutineContext = Job() + Dispatchers.Main

  @Inject lateinit var progressMoviesCase: ProgressMoviesMainCase

  override fun onHandleWork(intent: Intent) {
    val movieId = intent.getLongExtra(EXTRA_MOVIE_ID, -1)
    if (movieId == -1L) {
      val error = Throwable("Invalid ID.")
      return
    }

    runBlocking {
      progressMoviesCase.addToMyMovies(IdTmdb(movieId))
      (applicationContext as WidgetsProvider).requestMoviesWidgetsUpdate()
    }
  }

  override fun onDestroy() {
    cancel()
    super.onDestroy()
  }
}
