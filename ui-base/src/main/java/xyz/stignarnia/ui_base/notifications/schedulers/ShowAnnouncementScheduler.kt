package xyz.stignarnia.ui_base.notifications.schedulers

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.data_local.database.model.Show
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.notifications.NotificationChannel
import xyz.stignarnia.ui_base.notifications.AnnouncementWorker
import xyz.stignarnia.ui_model.ImageStatus
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.NotificationDelay
import xyz.stignarnia.ui_model.Translation
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShowAnnouncementScheduler @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val showsImagesProvider: ShowImagesProvider,
  private val translationsRepository: TranslationsRepository,
  private val mappers: Mappers,
) {

  companion object {
    const val ANNOUNCEMENT_WORK_TAG = "ANNOUNCEMENT_WORK_TAG"
    private const val ANNOUNCEMENT_STATIC_DELAY_MS = 60000 // 1 min
  }

  private val logFormatter by lazy { DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, HH:mm") }

  suspend fun scheduleAnnouncement(
    showDb: Show,
    episodeNumber: Int,
    episodeSeasonNumber: Int,
    episodeDate: ZonedDateTime,
    delay: NotificationDelay,
    language: String,
  ) {
    val show = mappers.show.fromDatabase(showDb)

    var translation: Translation? = null
    if (language != Config.DEFAULT_LANGUAGE) {
      translation = translationsRepository.loadTranslation(show, language, onlyLocal = true)
    }

    val data = Data.Builder().apply {
      val title = if (translation?.hasTitle == true) translation.title else show.title
      val episode = context.getString(R.string.textSeasonEpisode, episodeSeasonNumber, episodeNumber)

      putLong(AnnouncementWorker.DATA_SHOW_ID, showDb.idTmdb)
      putString(AnnouncementWorker.DATA_TITLE, "$title - $episode")
      putString(AnnouncementWorker.DATA_CHANNEL, NotificationChannel.EPISODES_ANNOUNCEMENTS.name)

      val stringResId = when (episodeNumber) {
        1 -> if (delay.isBefore()) R.string.textNewSeasonAvailableSoon else R.string.textNewSeasonAvailable
        else -> if (delay.isBefore()) R.string.textNewEpisodeAvailableSoon else R.string.textNewEpisodeAvailable
      }
      putString(AnnouncementWorker.DATA_CONTENT, context.getString(stringResId))

      val posterImage = showsImagesProvider.findCachedImage(show, ImageType.POSTER)
      if (posterImage.status == ImageStatus.AVAILABLE) {
        putString(AnnouncementWorker.DATA_IMAGE_URL, posterImage.fullFileUrl)
      } else {
        val fanartImage = showsImagesProvider.findCachedImage(show, ImageType.FANART)
        if (fanartImage.status == ImageStatus.AVAILABLE) {
          putString(AnnouncementWorker.DATA_IMAGE_URL, fanartImage.fullFileUrl)
        }
      }
    }

    val delayed = (episodeDate.toMillis() - nowUtcMillis()) + delay.delayMs + ANNOUNCEMENT_STATIC_DELAY_MS
    val request = OneTimeWorkRequestBuilder<AnnouncementWorker>()
      .setInputData(data.build())
      .setInitialDelay(delayed, TimeUnit.MILLISECONDS)
      .addTag(ANNOUNCEMENT_WORK_TAG)
      .build()

    WorkManager.getInstance(context).enqueue(request)

    val logTime = logFormatter.format(dateFromMillis(nowUtcMillis() + delayed))
    Timber.d("Notification set for ${show.title}: $logTime UTC")
  }
}
