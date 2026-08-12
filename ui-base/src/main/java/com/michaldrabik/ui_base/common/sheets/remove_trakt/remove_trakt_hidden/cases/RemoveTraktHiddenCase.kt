package com.michaldrabik.ui_base.common.sheets.remove_trakt.remove_trakt_hidden.cases

import com.michaldrabik.data_remote.trakt.AuthorizedTraktRemoteDataSource
import com.michaldrabik.data_remote.trakt.model.SyncExportItem
import com.michaldrabik.data_remote.trakt.model.SyncExportRequest
import com.michaldrabik.repository.UserTraktManager
import com.michaldrabik.ui_base.common.sheets.remove_trakt.RemoveTraktBottomSheet.Mode
import com.michaldrabik.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.delay
import javax.inject.Inject

private const val TRAKT_DELAY = 1500L

@ViewModelScoped
class RemoveTraktHiddenCase @Inject constructor(
  private val remoteSource: AuthorizedTraktRemoteDataSource,
  private val userManager: UserTraktManager,
) {

  suspend fun removeTraktHidden(
    tmdbIds: List<IdTmdb>,
    mode: Mode,
  ) {
    userManager.checkAuthorization()
    val items = tmdbIds.map { SyncExportItem.create(it.id) }

    when (mode) {
      Mode.SHOW -> {
        val request = SyncExportRequest(shows = items)
        remoteSource.deleteHiddenShow(request)
        delay(TRAKT_DELAY)
        remoteSource.deleteDroppedShow(request)
      }
      Mode.MOVIE -> {
        val request = SyncExportRequest(movies = items)
        remoteSource.deleteHiddenMovie(request)
      }
      else -> {
        throw IllegalStateException()
      }
    }
  }
}
