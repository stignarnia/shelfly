package xyz.stignarnia.uiSettings.sections.notifications.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.NotificationDelay
import xyz.stignarnia.uiModel.Settings
import javax.inject.Inject

@ViewModelScoped
class SettingsNotificationsMainCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val settingsRepository: SettingsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun getSettings(): Settings =
      withContext(dispatchers.IO) {
        settingsRepository.load()
      }

    suspend fun enableNotifications(enable: Boolean) {
      val settings = settingsRepository.load()
      settings.let {
        val new = it.copy(episodesNotificationsEnabled = enable)
        settingsRepository.update(new)

        announcementManager.refreshShowsAnnouncements()
        announcementManager.refreshMoviesAnnouncements()
      }
    }

    suspend fun setWhenToNotify(delay: NotificationDelay) {
      val settings = settingsRepository.load()
      settings.let {
        val new = it.copy(episodesNotificationsDelay = delay)
        settingsRepository.update(new)
        announcementManager.refreshShowsAnnouncements()
      }
    }
  }
