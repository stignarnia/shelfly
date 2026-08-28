package xyz.stignarnia.uiBase.notifications.schedulers

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.nowUtcDay
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.notifications.AnnouncementWorker
import xyz.stignarnia.uiBase.notifications.NotificationChannel
import xyz.stignarnia.uiModel.ImageStatus
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Translation
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MovieAnnouncementScheduler
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val moviesImagesProvider: MovieImagesProvider,
    private val translationsRepository: TranslationsRepository,
  ) {
    companion object {
      const val ANNOUNCEMENT_MOVIE_WORK_TAG = "ANNOUNCEMENT_MOVIE_WORK_TAG"
      private const val MOVIE_THRESHOLD_HOUR = 12
    }

    private val logFormatter by lazy { DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, HH:mm") }

    suspend fun scheduleAnnouncement(
      context: Context,
      movie: Movie,
      language: String,
    ) {
      var translation: Translation? = null
      if (language != Config.DEFAULT_LANGUAGE) {
        translation = translationsRepository.loadTranslation(movie, language, onlyLocal = true)
      }

      val data =
        Data.Builder().apply {
          putLong(AnnouncementWorker.DATA_MOVIE_ID, movie.tmdbId)
          putString(AnnouncementWorker.DATA_CHANNEL, NotificationChannel.MOVIES_ANNOUNCEMENTS.name)
          putString(AnnouncementWorker.DATA_TITLE, if (translation?.hasTitle == true) translation.title else movie.title)
          putString(AnnouncementWorker.DATA_CONTENT, context.getString(R.string.textNewMovieAvailable))

          val posterImage = moviesImagesProvider.findCachedImage(movie, ImageType.POSTER)
          if (posterImage.status == ImageStatus.AVAILABLE) {
            putString(AnnouncementWorker.DATA_IMAGE_URL, posterImage.fullFileUrl)
          } else {
            val fanartImage = moviesImagesProvider.findCachedImage(movie, ImageType.FANART)
            if (fanartImage.status == ImageStatus.AVAILABLE) {
              putString(AnnouncementWorker.DATA_IMAGE_URL, fanartImage.fullFileUrl)
            }
          }
        }

      val now = ZonedDateTime.now()
      val days = movie.released!!.toEpochDay() - nowUtcDay().toEpochDay()
      val offset = now.withHour(MOVIE_THRESHOLD_HOUR).withMinute(0).toMillis() - now.toMillis()
      val delayed = (days * TimeUnit.DAYS.toMillis(1)) + offset
      val request =
        OneTimeWorkRequestBuilder<AnnouncementWorker>()
          .setInputData(data.build())
          .setInitialDelay(delayed, TimeUnit.MILLISECONDS)
          .addTag(ANNOUNCEMENT_MOVIE_WORK_TAG)
          .build()

      WorkManager.getInstance(context).enqueue(request)

      val logTime = logFormatter.format(dateFromMillis(nowUtcMillis() + delayed))
      Timber.d("Notification set for ${movie.title}: $logTime UTC")
    }
  }
