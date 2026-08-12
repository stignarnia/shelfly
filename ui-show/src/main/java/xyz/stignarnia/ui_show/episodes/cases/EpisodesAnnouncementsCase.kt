package xyz.stignarnia.ui_show.episodes.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.notifications.AnnouncementManager
import xyz.stignarnia.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class EpisodesAnnouncementsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val showsRepository: ShowsRepository,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun refreshAnnouncements(idTmdb: IdTmdb) =
    withContext(dispatchers.IO) {
      val isMyShow = showsRepository.myShows.exists(idTmdb)
      if (isMyShow) {
        announcementManager.refreshShowsAnnouncements()
      }
    }
}
