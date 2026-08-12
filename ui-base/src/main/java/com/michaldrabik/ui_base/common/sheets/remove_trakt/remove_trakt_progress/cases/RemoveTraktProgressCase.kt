package com.michaldrabik.ui_base.common.sheets.remove_trakt.remove_trakt_progress.cases

import com.michaldrabik.data_remote.trakt.AuthorizedTraktRemoteDataSource
import com.michaldrabik.data_remote.trakt.model.SyncExportItem
import com.michaldrabik.data_remote.trakt.model.SyncExportRequest
import com.michaldrabik.repository.EpisodesManager
import com.michaldrabik.repository.UserTraktManager
import com.michaldrabik.ui_base.common.sheets.remove_trakt.RemoveTraktBottomSheet.Mode
import com.michaldrabik.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class RemoveTraktProgressCase @Inject constructor(
  private val remoteSource: AuthorizedTraktRemoteDataSource,
  private val userManager: UserTraktManager,
  private val episodesManager: EpisodesManager,
) {

  suspend fun removeTraktProgress(
    tmdbIds: List<IdTmdb>,
    mode: Mode,
  ) {
    userManager.checkAuthorization()
    val items = tmdbIds.map { SyncExportItem.create(it.id) }

    val request = when (mode) {
      Mode.SHOW -> SyncExportRequest(shows = items)
      Mode.MOVIE -> SyncExportRequest(movies = items)
      Mode.EPISODE -> SyncExportRequest(episodes = items)
    }

    remoteSource.postDeleteProgress(request)
    if (mode == Mode.SHOW && tmdbIds.isNotEmpty()) {
      episodesManager.setAllUnwatched(tmdbIds.first())
    }
  }
}
