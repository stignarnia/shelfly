package xyz.stignarnia.ui_base.sync.runners

import xyz.stignarnia.common.ConfigVariant.SHOW_SYNC_COOLDOWN
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.EpisodesSyncLog
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_model.ShowStatus.CANCELED
import xyz.stignarnia.ui_model.ShowStatus.ENDED
import xyz.stignarnia.ui_model.ShowStatus.UNKNOWN
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is responsible for fetching and syncing missing/updated episodes data for current progress shows.
 */
@Singleton
class ShowsSyncRunner @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
  private val episodesManager: EpisodesManager,
  private val showsRepository: ShowsRepository,
) {

  companion object {
    private const val DELAY_MS = 10L
  }

  suspend fun run(): Int {
    Timber.i("Shows sync initialized.")

    val myShows = showsRepository.myShows.loadAll()
    val watchlistShows = showsRepository.watchlistShows.loadAll()
    val watchlistShowsIds = watchlistShows.map { it.tmdbId }

    val showsToSync = (myShows + watchlistShows)
      .filter { it.status !in arrayOf(ENDED, CANCELED, UNKNOWN) }

    Timber.i("Shows to sync: ${showsToSync.size}.")
    if (showsToSync.isEmpty()) {
      Timber.i("Nothing to sync. Stopping...")
      return 0
    }

    var syncCount = 0
    val syncLog = localSource.episodesSyncLog.getAll()
    showsToSync.forEach { show ->
      val isInWatchlist = show.tmdbId in watchlistShowsIds

      val lastSync = syncLog.find { it.idTmdb == show.tmdbId }?.syncedAt ?: 0
      if (nowUtcMillis() - lastSync < SHOW_SYNC_COOLDOWN) {
        Timber.i("${show.title} is on cooldown. No need to sync.")
        return@forEach
      }

      try {
        Timber.i("Syncing ${show.title}(${show.ids.tmdb}) details...")
        showsRepository.detailsShow.load(show.ids.tmdb, force = true)
        syncCount++
        Timber.i("${show.title}(${show.ids.tmdb}) show synced.")
      } catch (t: Throwable) {
        Timber.e("${show.title}(${show.ids.tmdb}) show sync error. Skipping... \n$t")
      }

      if (isInWatchlist) {
        localSource.episodesSyncLog.upsert(EpisodesSyncLog(show.tmdbId, nowUtcMillis()))
      } else {
        try {
          Timber.i("Syncing ${show.title}(${show.ids.tmdb}) episodes...")

          val remoteSeasons = remoteSource.tmdb
            .fetchSeasons(show.tmdbId)
            .map { mappers.season.fromNetwork(it) }
          episodesManager.invalidateSeasons(show, remoteSeasons)
          syncCount++

          Timber.i("${show.title}(${show.ids.tmdb}) episodes synced.")
        } catch (t: Throwable) {
          Timber.e("${show.title}(${show.ids.tmdb}) episodes sync error. Skipping... \n$t")
        } finally {
          delay(DELAY_MS)
        }
      }
    }

    return syncCount
  }
}
