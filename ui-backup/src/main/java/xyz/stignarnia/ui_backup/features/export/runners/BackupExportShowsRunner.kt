package xyz.stignarnia.ui_backup.features.export.runners

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.dateIsoStringFromMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.shows.ratings.ShowsRatingsRepository
import xyz.stignarnia.ui_backup.model.BackupEpisode
import xyz.stignarnia.ui_backup.model.BackupEpisodeRating
import xyz.stignarnia.ui_backup.model.BackupSeason
import xyz.stignarnia.ui_backup.model.BackupSeasonRating
import xyz.stignarnia.ui_backup.model.BackupShow
import xyz.stignarnia.ui_backup.model.BackupShowRating
import xyz.stignarnia.ui_backup.model.BackupShows
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal class BackupExportShowsRunner @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val onHoldItemsRepository: OnHoldItemsRepository,
  private val ratingsRepository: ShowsRatingsRepository,
) : BackupExportRunner<BackupShows>() {

  override suspend fun run(): BackupShows {
    Timber.d("Initialized.")
    return runExport()
      .also {
        Timber.d("Success.")
      }
  }

  private suspend fun runExport(): BackupShows =
    withContext(dispatchers.IO) {
      val backupShowsCollection = exportShowsCollection()
      val backupShowsProgress = exportShowsProgress()
      val backupEpisodesProgress = exportEpisodesProgress()

      val backupShowsRatings = exportShowsRatings()
      val backupSeasonsRatings = exportSeasonsRatings()
      val backupEpisodesRatings = exportEpisodesRatings()

      BackupShows(
        collectionHistory = backupShowsCollection.collectionHistory,
        collectionWatchlist = backupShowsCollection.collectionWatchlist,
        collectionHidden = backupShowsCollection.collectionHidden,
        progressSeasons = backupEpisodesProgress.progressSeasons,
        progressEpisodes = backupEpisodesProgress.progressEpisodes,
        progressPinned = backupShowsProgress.progressPinned,
        progressOnHold = backupShowsProgress.progressOnHold,
        ratingsShows = backupShowsRatings.ratingsShows,
        ratingsSeasons = backupSeasonsRatings.ratingsSeasons,
        ratingsEpisodes = backupEpisodesRatings.ratingsEpisodes,
      )
    }

  private suspend fun exportShowsCollection(): BackupShows =
    withContext(dispatchers.IO) {
      val myShowsAsync = async { localSource.myShows.getAll() }
      val watchlistShowsAsync = async { localSource.watchlistShows.getAll() }
      val hiddenShowsAsync = async { localSource.archiveShows.getAll() }

      val myShows = myShowsAsync.await()
      val watchlistShows = watchlistShowsAsync.await()
      val hiddenShows = hiddenShowsAsync.await()

      val collectionMyShows = myShows.map {
        BackupShow(
          tmdbId = it.idTmdb,
          title = it.title,
          addedAt = dateIsoStringFromMillis(it.createdAt),
          updatedAt = dateIsoStringFromMillis(it.updatedAt),
        )
      }
      val collectionWatchlist = watchlistShows.map {
        BackupShow(
          tmdbId = it.idTmdb,
          title = it.title,
          addedAt = dateIsoStringFromMillis(it.createdAt),
          updatedAt = dateIsoStringFromMillis(it.updatedAt),
        )
      }
      val collectionHidden = hiddenShows.map {
        BackupShow(
          tmdbId = it.idTmdb,
          title = it.title,
          addedAt = dateIsoStringFromMillis(it.createdAt),
          updatedAt = dateIsoStringFromMillis(it.updatedAt),
        )
      }

      BackupShows(
        collectionHistory = collectionMyShows,
        collectionWatchlist = collectionWatchlist,
        collectionHidden = collectionHidden,
      )
    }

  private suspend fun exportEpisodesProgress(): BackupShows =
    withContext(dispatchers.IO) {
      val watchedEpisodesAsync = async { localSource.episodes.getAllWatched() }
      val watchedSeasonsAsync = async { localSource.seasons.getAllWatched() }

      val watchedEpisodes = watchedEpisodesAsync.await()
      val watchedSeasons = watchedSeasonsAsync.await()

      val seasonsIds = watchedSeasons.map { it.idShowTmdb }.distinct()
      val shows = localSource.shows.getAllTmdbIds(tmdbIds = seasonsIds)

      val progressSeasons = watchedSeasons.map { season ->
        BackupSeason(
          showTmdbId = shows.getOrDefault(season.idShowTmdb, -1),
          seasonNumber = season.seasonNumber,
        )
      }

      val progressEpisodes = watchedEpisodes.map { episode ->
        BackupEpisode(
          showTmdbId = episode.idShowTmdb,
          episodeNumber = episode.episodeNumber,
          seasonNumber = episode.seasonNumber,
          addedAt = episode.lastWatchedAt?.let { dateIsoStringFromMillis(it.toMillis()) },
        )
      }

      BackupShows(
        progressSeasons = progressSeasons,
        progressEpisodes = progressEpisodes,
      )
    }

  private suspend fun exportShowsProgress(): BackupShows =
    withContext(dispatchers.IO) {
      val pinnedIds = pinnedItemsRepository.getAllShows()
      val onHoldIds = onHoldItemsRepository.getAll().map { it.id }
      BackupShows(
        progressPinned = pinnedIds,
        progressOnHold = onHoldIds,
      )
    }

  // Ratings

  private suspend fun exportShowsRatings(): BackupShows =
    withContext(dispatchers.IO) {
      val showsRatings = ratingsRepository.loadShowsRatings().map {
        BackupShowRating(
          tmdbId = it.idTmdb.id,
          rating = it.rating,
          ratedAt = dateIsoStringFromMillis(it.ratedAt.toMillis()),
        )
      }

      BackupShows(
        ratingsShows = showsRatings,
      )
    }

  // Season and episode ratings are already stored under their show, so the
  // backup entry is a straight copy - no lookup can fail here any more.

  private suspend fun exportSeasonsRatings(): BackupShows =
    withContext(dispatchers.IO) {
      val seasonsRatings = ratingsRepository.loadSeasonsRatings().map { rating ->
        BackupSeasonRating(
          showTmdbId = rating.idTmdb,
          seasonNumber = rating.seasonNumber,
          rating = rating.rating,
          ratedAt = dateIsoStringFromMillis(rating.ratedAt.toMillis()),
        )
      }

      BackupShows(
        ratingsSeasons = seasonsRatings,
      )
    }

  private suspend fun exportEpisodesRatings(): BackupShows =
    withContext(dispatchers.IO) {
      val episodesRatings = ratingsRepository.loadEpisodesRatings().map { rating ->
        BackupEpisodeRating(
          showTmdbId = rating.idTmdb,
          seasonNumber = rating.seasonNumber,
          episodeNumber = rating.episodeNumber,
          rating = rating.rating,
          ratedAt = dateIsoStringFromMillis(rating.ratedAt.toMillis()),
        )
      }

      BackupShows(
        ratingsEpisodes = episodesRatings,
      )
    }
}
