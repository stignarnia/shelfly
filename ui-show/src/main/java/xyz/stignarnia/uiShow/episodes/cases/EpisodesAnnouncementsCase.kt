package xyz.stignarnia.uiShow.episodes.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

@ViewModelScoped
class EpisodesAnnouncementsCase
  @Inject
  constructor(
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
