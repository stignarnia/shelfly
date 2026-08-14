package xyz.stignarnia.ui_backup.features.import_.runners

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcDateTime
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.ArchiveShow
import xyz.stignarnia.data_local.database.model.Episode
import xyz.stignarnia.data_local.database.model.MyShow
import xyz.stignarnia.data_local.database.model.Rating
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.data_local.database.model.WatchlistShow
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.repository.shows.ratings.ShowsRatingsRepository
import xyz.stignarnia.ui_backup.features.import_.model.BackupImportStatus.Importing
import xyz.stignarnia.ui_backup.model.BackupShow
import xyz.stignarnia.ui_backup.model.BackupShows
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_model.IdTmdb
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

internal class BackupImportShowsRunner @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val remoteSource: RemoteDataSource,
  private val showsRepository: ShowsRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
  private val onHoldItemsRepository: OnHoldItemsRepository,
  private val ratingsRepository: ShowsRatingsRepository,
  private val episodesManager: EpisodesManager,
  private val mappers: Mappers,
  private val transactions: TransactionsProvider,
) : BackupImportRunner<BackupShows>() {

  override suspend fun run(backup: BackupShows) {
    Timber.d("Initialized.")
    runImport(backup)
      .also {
        Timber.d("Success.")
      }
  }

  private suspend fun runImport(backup: BackupShows) {
    withContext(dispatchers.IO) {
      importShowsCollection(backup)

      importShowsPinned(backup)
      importShowsOnHold(backup)

      importShowsRatings(backup)
      importSeasonsRatings(backup)
      importEpisodesRatings(backup)
    }
  }

  private suspend fun importShowsCollection(backup: BackupShows) {
    withContext(dispatchers.IO) {
      val localCollection = showsRepository
        .loadCollection()
        .map { it.tmdbId }

      importMyShows(backup, localCollection)
      importWatchlistShows(backup, localCollection)
      importHiddenShows(backup, localCollection)
    }
  }

  private suspend fun importMyShows(
    backupShows: BackupShows,
    localCollection: List<Long>,
  ) {
    for (show in backupShows.collectionHistory) {
      Timber.d("Importing show ${show.tmdbId} ...")
      statusListener?.invoke(Importing(show.title))

      if (localCollection.contains(show.tmdbId)) {
        if (showsRepository.myShows.exists(IdTmdb(show.tmdbId))) {
          importExistingMyShowEpisodes(IdTmdb(show.tmdbId), backupShows)
          continue
        }
        Timber.d("Show already in collection. Skipping.")
        continue
      }

      val showDetails = localSource.shows.getById(show.tmdbId)
      if (showDetails == null) {
        if (!fetchShowDetails(show)) {
          continue
        }
      }

      val addedAt = show.addedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
      val updatedAt = show.updatedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
      val myShows = MyShow.fromTmdbId(
        tmdbId = show.tmdbId,
        createdAt = addedAt,
        updatedAt = addedAt,
        watchedAt = updatedAt,
      )

      Timber.d("New show in My Shows. Importing season, episodes ...")
      val (seasons, episodes) = loadSeasonsEpisodes(show.tmdbId, backupShows)

      transactions.withTransaction {
        localSource.seasons.upsert(seasons)
        localSource.episodes.upsert(episodes)
        localSource.myShows.insert(listOf(myShows))
      }

      Timber.d("Added to My Shows ${show.tmdbId} ...")
    }
  }

  private suspend fun importWatchlistShows(
    backupShows: BackupShows,
    localCollection: List<Long>,
  ) {
    for (show in backupShows.collectionWatchlist) {
      Timber.d("Importing show ${show.tmdbId} ...")
      statusListener?.invoke(Importing(show.title))

      if (localCollection.contains(show.tmdbId)) {
        Timber.d("Show already in collection. Skipping.")
        continue
      }

      val showDetails = localSource.shows.getById(show.tmdbId)
      if (showDetails == null) {
        if (!fetchShowDetails(show)) {
          continue
        }
      }

      val timestamp = show.addedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
      val watchlistShow = WatchlistShow.fromTmdbId(show.tmdbId, timestamp)
      localSource.watchlistShows.insert(watchlistShow)

      Timber.d("Added to Watchlist ${show.tmdbId} ...")
    }
  }

  private suspend fun importHiddenShows(
    backupShows: BackupShows,
    localCollection: List<Long>,
  ) {
    for (show in backupShows.collectionHidden) {
      Timber.d("Importing show ${show.tmdbId} ...")
      statusListener?.invoke(Importing(show.title))

      if (localCollection.contains(show.tmdbId)) {
        Timber.d("Show already in collection. Skipping.")
        continue
      }

      val showDetails = localSource.shows.getById(show.tmdbId)
      if (showDetails == null) {
        if (!fetchShowDetails(show)) {
          continue
        }
      }

      val timestamp = show.addedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
      val hiddenShow = ArchiveShow.fromTmdbId(show.tmdbId, timestamp)
      localSource.archiveShows.insert(hiddenShow)

      Timber.d("Added to Hidden ${show.tmdbId} ...")
    }
  }

  private suspend fun importShowsPinned(backup: BackupShows) {
    withContext(dispatchers.IO) {
      val localPinned = pinnedItemsRepository.getAllShows()
      for (pinned in backup.progressPinned) {
        if (!localPinned.contains(pinned)) {
          pinnedItemsRepository.addShowPinnedItem(IdTmdb(pinned))
        }
      }
    }
  }

  private suspend fun importShowsOnHold(backup: BackupShows) {
    withContext(dispatchers.IO) {
      val localOnHold = onHoldItemsRepository.getAll().map { it.id }
      for (onHoldShow in backup.progressOnHold) {
        if (!localOnHold.contains(onHoldShow)) {
          onHoldItemsRepository.addItem(IdTmdb(onHoldShow))
        }
      }
    }
  }

  // An import never overwrites a rating the user already has, so every runner
  // below checks the full key first - show or movie id plus, for a season or an
  // episode, the numbers underneath it.

  private suspend fun importShowsRatings(backup: BackupShows) {
    withContext(dispatchers.IO) {
      val localRatings = ratingsRepository.loadShowsRatings()

      for (rating in backup.ratingsShows) {
        if (localRatings.any { it.idTmdb.id == rating.tmdbId }) {
          continue
        }

        val entity = Rating(
          idTmdb = rating.tmdbId,
          type = Rating.TYPE_SHOW,
          rating = rating.rating,
          ratedAt = rating.ratedAt.toUtcDateTime() ?: nowUtc(),
          createdAt = nowUtc(),
          updatedAt = nowUtc(),
        )

        localSource.ratings.replace(entity)
      }
    }
  }

  private suspend fun importSeasonsRatings(backup: BackupShows) {
    withContext(dispatchers.IO) {
      val localKeys = ratingsRepository
        .loadSeasonsRatings()
        .mapTo(mutableSetOf()) { it.idTmdb to it.seasonNumber }

      for (rating in backup.ratingsSeasons) {
        val key = rating.showTmdbId to rating.seasonNumber
        if (!localKeys.add(key)) {
          continue
        }

        val entity = Rating(
          idTmdb = rating.showTmdbId,
          type = Rating.TYPE_SEASON,
          rating = rating.rating,
          seasonNumber = rating.seasonNumber,
          ratedAt = rating.ratedAt.toUtcDateTime() ?: nowUtc(),
          createdAt = nowUtc(),
          updatedAt = nowUtc(),
        )

        localSource.ratings.replace(entity)
      }
    }
  }

  private suspend fun importEpisodesRatings(backup: BackupShows) {
    withContext(dispatchers.IO) {
      val localKeys = ratingsRepository
        .loadEpisodesRatings()
        .mapTo(mutableSetOf()) { Triple(it.idTmdb, it.seasonNumber, it.episodeNumber) }

      for (rating in backup.ratingsEpisodes) {
        val key = Triple(rating.showTmdbId, rating.seasonNumber, rating.episodeNumber)
        if (!localKeys.add(key)) {
          continue
        }

        val entity = Rating(
          idTmdb = rating.showTmdbId,
          type = Rating.TYPE_EPISODE,
          rating = rating.rating,
          seasonNumber = rating.seasonNumber,
          episodeNumber = rating.episodeNumber,
          ratedAt = rating.ratedAt.toUtcDateTime() ?: nowUtc(),
          createdAt = nowUtc(),
          updatedAt = nowUtc(),
        )

        localSource.ratings.replace(entity)
      }
    }
  }

  private suspend fun importExistingMyShowEpisodes(
    showId: IdTmdb,
    backup: BackupShows,
  ) {
    Timber.d("Show already in My Shows. Importing episodes ...")
    withContext(dispatchers.IO) {
      val show = localSource.shows.getById(showId.id) ?: return@withContext
      val importEpisodes = backup.progressEpisodes
        .filter { it.showTmdbId == showId.id }

      val localEpisodesAsync = async { localSource.episodes.getAllByShowId(show.idTmdb) }
      val localEpisodes = localEpisodesAsync.await()

      if (localEpisodes.isEmpty()) {
        return@withContext
      }

      for (importEpisode in importEpisodes) {
        val localEpisode = localEpisodes
          .firstOrNull {
            it.seasonNumber == importEpisode.seasonNumber && it.episodeNumber == importEpisode.episodeNumber
          }

        if (localEpisode != null && !localEpisode.isWatched) {
          episodesManager.setEpisodeWatched(
            showId = showId,
            seasonId = localEpisode.idSeason,
            episodeId = localEpisode.idTmdb,
            customDate = importEpisode.addedAt?.toUtcDateTime(),
          )
        }
      }
    }
  }

  private suspend fun loadSeasonsEpisodes(
    showId: Long,
    backupShows: BackupShows,
  ): Pair<List<Season>, List<Episode>> =
    coroutineScope {
      val remoteSeasons = remoteSource.tmdb.fetchSeasons(showId)

      val localEpisodesAsync = async { localSource.episodes.getAllWatchedIdsForShows(listOf(showId)) }
      val localSeasonsAsync = async { localSource.seasons.getAllWatchedIdsForShows(listOf(showId)) }
      val localEpisodesIds = localEpisodesAsync.await()
      val localSeasonsIds = localSeasonsAsync.await()

      val backupSeason = backupShows.progressSeasons.filter { it.showTmdbId == showId }
      val backupEpisodes = backupShows.progressEpisodes.filter { it.showTmdbId == showId }

      val seasons = remoteSeasons
        .filterNot { localSeasonsIds.contains(it.ids?.tmdb) }
        .map { mappers.season.fromNetwork(it) }
        .map { remoteSeason ->
          val isWatchedNumber = backupSeason
            .any { it.seasonNumber == remoteSeason.number }

          val isWatchedSize = backupEpisodes
            .count { it.seasonNumber == remoteSeason.number } == remoteSeason.episodes.size

          mappers.season.toDatabase(
            season = remoteSeason,
            showId = IdTmdb(showId),
            isWatched = isWatchedNumber && isWatchedSize,
          )
        }

      val episodes = remoteSeasons.flatMap { season ->
        season.episodes
          ?.filterNot { localEpisodesIds.contains(it.ids?.tmdb) }
          ?.map { episode ->
            val importEpisode = backupEpisodes
              .find {
                it.seasonNumber == episode.season &&
                  it.episodeNumber == episode.number
              }

            val watchedAt = importEpisode?.addedAt?.toUtcDateTime()
            val exportedAt = importEpisode?.let {
              it.addedAt?.toUtcDateTime() ?: nowUtc()
            }

            mappers.episode.toDatabase(
              showId = IdTmdb(showId),
              season = mappers.season.fromNetwork(season),
              episode = mappers.episode.fromNetwork(episode),
              isWatched = importEpisode != null,
              lastExportedAt = exportedAt,
              lastWatchedAt = watchedAt,
            )
          } ?: emptyList()
      }

      Pair(seasons, episodes)
    }

  private suspend fun fetchShowDetails(show: BackupShow): Boolean {
    Timber.d("Fetching remote show details for ${show.tmdbId} ...")
    return try {
      showsRepository.detailsShow.load(IdTmdb(show.tmdbId), force = true)
      true
    } catch (error: Throwable) {
      rethrowCancellation(error) {
        if (error is HttpException && error.code() == 404) {
          Timber.w("Failed to fetch show: ${show.tmdbId} ${show.title}")
        }
      }
      false
    }
  }
}
