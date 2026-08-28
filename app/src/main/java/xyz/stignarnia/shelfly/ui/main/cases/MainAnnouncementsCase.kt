package xyz.stignarnia.shelfly.ui.main.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import javax.inject.Inject

@ViewModelScoped
class MainAnnouncementsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun refreshAnnouncements() {
      withContext(dispatchers.IO) {
        announcementManager.refreshShowsAnnouncements()
        announcementManager.refreshMoviesAnnouncements()
      }
    }
  }
