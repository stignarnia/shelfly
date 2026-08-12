package xyz.stignarnia.ui_base.notifications

import android.content.Context
import androidx.work.WorkManager
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcDay
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toZonedDateTime
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.notifications.schedulers.MovieAnnouncementScheduler
import xyz.stignarnia.ui_base.notifications.schedulers.MovieAnnouncementScheduler.Companion.ANNOUNCEMENT_MOVIE_WORK_TAG
import xyz.stignarnia.ui_base.notifications.schedulers.ShowAnnouncementScheduler
import xyz.stignarnia.ui_base.notifications.schedulers.ShowAnnouncementScheduler.Companion.ANNOUNCEMENT_WORK_TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val mappers: Mappers,
  private val localSource: LocalDataSource,
  private val settingsRepository: SettingsRepository,
  private val translationsRepository: TranslationsRepository,
  private val onHoldItemsRepository: OnHoldItemsRepository,
  private val showAnnouncementScheduler: ShowAnnouncementScheduler,
  private val movieAnnouncementScheduler: MovieAnnouncementScheduler,
) {

  companion object {
    private const val MOVIE_MIN_THRESHOLD_DAYS = 30
    private const val MOVIE_THRESHOLD_HOUR = 12
  }

  private val logFormatter by lazy { DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, HH:mm") }

  suspend fun refreshShowsAnnouncements() {
    Timber.d("Refreshing shows announcements")

    val now = nowUtc()
    val nowMillis = now.toMillis()
    val limit = now.plusMonths(3)

    WorkManager
      .getInstance(context)
      .cancelAllWorkByTag(ANNOUNCEMENT_WORK_TAG)

    Timber.d("Current time: ${logFormatter.format(now)} UTC")

    val settings = settingsRepository.load()
    if (!settings.episodesNotificationsEnabled) {
      Timber.d("Episodes announcements are disabled. Exiting...")
      return
    }

    val myShows = localSource.myShows
      .getAll()
      .distinctBy { it.idTmdb }

    val watchlistShows = localSource.watchlistShows
      .getAll()
      .distinctBy { it.idTmdb }

    if (myShows.isEmpty() && watchlistShows.isEmpty()) {
      Timber.d("Nothing to process. Exiting...")
      return
    }

    val language = translationsRepository.getLanguage()
    val delay = settings.episodesNotificationsDelay
    val onHoldIds = onHoldItemsRepository.getAll().map { it.id }

    myShows
      .forEach { show ->
        Timber.d("Processing ${show.title} (${show.idTmdb})")

        if (onHoldIds.contains(show.idTmdb)) {
          Timber.d("${show.title} (${show.idTmdb}) is on hold. Skipping...")
          return@forEach
        }

        val fromTime = if (delay.isBefore()) nowMillis else nowMillis - delay.delayMs
        val episode = localSource.episodes.getFirstUnwatched(show.idTmdb, fromTime, limit.toMillis())
        episode?.firstAired?.let { airDate ->
          when {
            delay.isBefore() -> {
              if (airDate.toMillis() + delay.delayMs >= nowMillis) {
                showAnnouncementScheduler.scheduleAnnouncement(
                  showDb = show,
                  episodeNumber = episode.episodeNumber,
                  episodeSeasonNumber = episode.seasonNumber,
                  episodeDate = episode.firstAired!!,
                  delay = delay,
                  language = language,
                )
              } else {
                Timber.d("Time with delay included has already passed.")
              }
            }
            else -> {
              showAnnouncementScheduler.scheduleAnnouncement(
                showDb = show,
                episodeNumber = episode.episodeNumber,
                episodeSeasonNumber = episode.seasonNumber,
                episodeDate = episode.firstAired!!,
                delay = delay,
                language = language,
              )
            }
          }
        }
      }

    for (show in watchlistShows) {
      Timber.d("Processing Watchlist ${show.title} (${show.idTmdb})")

      val fromTime = if (delay.isBefore()) nowMillis else nowMillis - delay.delayMs
      val airDate = show.firstAired.toZonedDateTime() ?: ZonedDateTime.now().minusYears(1)

      if (airDate.toMillis() <= fromTime) {
        continue
      }

      if (delay.isBefore()) {
        if (airDate.toMillis() + delay.delayMs >= nowMillis) {
          showAnnouncementScheduler.scheduleAnnouncement(
            showDb = show,
            episodeNumber = 1,
            episodeSeasonNumber = 1,
            episodeDate = airDate,
            delay = delay,
            language = language,
          )
        } else {
          Timber.d("Time with delay included has already passed.")
        }
      } else {
        showAnnouncementScheduler.scheduleAnnouncement(
          showDb = show,
          episodeNumber = 1,
          episodeSeasonNumber = 1,
          episodeDate = airDate,
          delay = delay,
          language = language,
        )
      }
    }
  }

  suspend fun refreshMoviesAnnouncements() {
    Timber.d("Refreshing movies announcements")

    val now = nowUtc()
    Timber.d("Current time: ${logFormatter.format(now)} UTC")

    WorkManager.getInstance(context).cancelAllWorkByTag(ANNOUNCEMENT_MOVIE_WORK_TAG)

    if (!settingsRepository.isMoviesEnabled) {
      Timber.d("Movies disabled. Skipping...")
      return
    }

    val movies = localSource.watchlistMovies
      .getAll()
      .distinctBy { it.idTmdb }
      .map { mappers.movie.fromDatabase(it) }

    if (movies.isEmpty()) {
      Timber.d("Nothing to process. Exiting...")
      return
    }

    val language = translationsRepository.getLanguage()
    movies
      .filter {
        Timber.d("Processing ${it.title} (${it.tmdbId})")
        it.released != null &&
          (!it.hasAired() || it.isToday()) &&
          it.released!!.toEpochDay() - nowUtcDay().toEpochDay() < MOVIE_MIN_THRESHOLD_DAYS &&
          // We want movies notifications to come out the release day at 12:00 local time
          ZonedDateTime.now().hour < MOVIE_THRESHOLD_HOUR
      }.forEach {
        movieAnnouncementScheduler.scheduleAnnouncement(context, it, language)
      }
  }
}
