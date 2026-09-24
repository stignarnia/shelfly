package xyz.stignarnia.uiBackup.features.imports.runners

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcDateTime
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.ArchiveShow
import xyz.stignarnia.dataLocal.database.model.Episode
import xyz.stignarnia.dataLocal.database.model.MyShow
import xyz.stignarnia.dataLocal.database.model.Rating
import xyz.stignarnia.dataLocal.database.model.Season
import xyz.stignarnia.dataLocal.database.model.WatchlistShow
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.repository.shows.ratings.ShowsRatingsRepository
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Importing
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportText
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedEpisode
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedSeason
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedShow
import xyz.stignarnia.uiBackup.model.BackupEpisode
import xyz.stignarnia.uiBackup.model.BackupSeason
import xyz.stignarnia.uiBackup.model.BackupShow
import xyz.stignarnia.uiBackup.model.BackupShows
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiModel.IdTmdb
import java.io.IOException
import javax.inject.Inject

internal class BackupImportShowsRunner
  @Inject
  constructor(
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
    val failedShows = mutableListOf<BackupUnmatchedShow>()

    override suspend fun run(
      backup: BackupShows,
      startCount: Int,
      total: Int,
    ): Int {
      failedShows.clear()
      Timber.d("Initialized.")
      return runImport(backup, startCount, total)
        .also {
          Timber.d("Success.")
        }
    }

    private suspend fun runImport(
      backup: BackupShows,
      startCount: Int,
      total: Int,
    ): Int =
      withContext(dispatchers.IO) {
        val countAfterCollection = importShowsCollection(backup, startCount, total)

        importShowsPinned(backup)
        importShowsOnHold(backup)

        importShowsRatings(backup)
        importSeasonsRatings(backup)
        importEpisodesRatings(backup)

        countAfterCollection
      }

    private suspend fun importShowsCollection(
      backup: BackupShows,
      startCount: Int,
      total: Int,
    ): Int =
      withContext(dispatchers.IO) {
        val localCollection =
          showsRepository
            .loadCollection()
            .map { it.tmdbId }
            .toMutableSet()

        var current = startCount

        current = importMyShows(backup, localCollection, current, total)
        current = importWatchlistShows(backup, localCollection, current, total)
        importHiddenShows(backup, localCollection, current, total)
      }

    private suspend fun importMyShows(
      backupShows: BackupShows,
      localCollection: MutableSet<Long>,
      startCount: Int,
      total: Int,
    ): Int {
      var current = startCount

      for (show in backupShows.collectionHistory) {
        current++
        updateProgress(show.title, current, total)
        Timber.d("Importing show ${show.tmdbId} ...")

        if (localCollection.contains(show.tmdbId)) {
          if (showsRepository.myShows.exists(IdTmdb(show.tmdbId))) {
            importExistingShowEpisodes(show, backupShows)
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
        val myShows =
          MyShow.fromTmdbId(
            tmdbId = show.tmdbId,
            createdAt = addedAt,
            updatedAt = addedAt,
            watchedAt = updatedAt,
          )

        Timber.d("New show in My Shows. Importing season, episodes ...")
        val (seasons, episodes) = loadSeasonsEpisodes(show, backupShows)

        transactions.withTransaction {
          localSource.seasons.upsert(seasons)
          localSource.episodes.upsert(episodes)
          localSource.myShows.insert(listOf(myShows))
        }

        localCollection.add(show.tmdbId)
        Timber.d("Added to My Shows ${show.tmdbId} ...")
      }
      return current
    }

    private suspend fun importWatchlistShows(
      backupShows: BackupShows,
      localCollection: MutableSet<Long>,
      startCount: Int,
      total: Int,
    ): Int {
      var current = startCount

      for (show in backupShows.collectionWatchlist) {
        current++
        updateProgress(show.title, current, total)
        Timber.d("Importing show ${show.tmdbId} ...")

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

        localCollection.add(show.tmdbId)
        Timber.d("Added to Watchlist ${show.tmdbId} ...")
      }
      return current
    }

    private suspend fun importHiddenShows(
      backupShows: BackupShows,
      localCollection: MutableSet<Long>,
      startCount: Int,
      total: Int,
    ): Int {
      var current = startCount

      for (show in backupShows.collectionHidden) {
        current++
        updateProgress(show.title, current, total)
        Timber.d("Importing show ${show.tmdbId} ...")

        if (localCollection.contains(show.tmdbId)) {
          if (showsRepository.hiddenShows.exists(IdTmdb(show.tmdbId))) {
            importExistingShowEpisodes(show, backupShows)
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

        val timestamp = show.addedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
        val hiddenShow = ArchiveShow.fromTmdbId(show.tmdbId, timestamp)

        Timber.d("New show in Hidden. Importing season, episodes ...")
        val (seasons, episodes) = loadSeasonsEpisodes(show, backupShows)

        transactions.withTransaction {
          localSource.seasons.upsert(seasons)
          localSource.episodes.upsert(episodes)
          localSource.archiveShows.insert(hiddenShow)
        }

        localCollection.add(show.tmdbId)
        Timber.d("Added to Hidden ${show.tmdbId} ...")
      }
      return current
    }

    private suspend fun updateProgress(title: String, current: Int, total: Int) {
      statusListener?.invoke(Importing(title, current = current, total = total))
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

    // An import never overwrites a rating the user already has, so every runner below checks the full key first - show or movie id plus, for a season or an episode, the numbers underneath it.

    private suspend fun importShowsRatings(backup: BackupShows) {
      withContext(dispatchers.IO) {
        val localRatings = ratingsRepository.loadShowsRatings()

        for (rating in backup.ratingsShows) {
          if (localRatings.any { it.idTmdb.id == rating.tmdbId }) {
            continue
          }

          val entity =
            Rating(
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
        val localKeys =
          ratingsRepository
            .loadSeasonsRatings()
            .mapTo(mutableSetOf()) { it.idTmdb to it.seasonNumber }

        for (rating in backup.ratingsSeasons) {
          val key = rating.showTmdbId to rating.seasonNumber
          if (!localKeys.add(key)) {
            continue
          }

          val entity =
            Rating(
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
        val localKeys =
          ratingsRepository
            .loadEpisodesRatings()
            .mapTo(mutableSetOf()) { Triple(it.idTmdb, it.seasonNumber, it.episodeNumber) }

        for (rating in backup.ratingsEpisodes) {
          val key = Triple(rating.showTmdbId, rating.seasonNumber, rating.episodeNumber)
          if (!localKeys.add(key)) {
            continue
          }

          val entity =
            Rating(
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

    private suspend fun importExistingShowEpisodes(
      show: BackupShow,
      backup: BackupShows,
    ) {
      Timber.d("Show already in collection. Importing episodes ...")
      withContext(dispatchers.IO) {
        val showEntity = localSource.shows.getById(show.tmdbId) ?: return@withContext
        val importEpisodes =
          backup.progressEpisodes
            .filter { it.showTmdbId == show.tmdbId }

        val localEpisodesAsync = async { localSource.episodes.getAllByShowId(showEntity.idTmdb) }
        val localEpisodes = localEpisodesAsync.await()

        if (localEpisodes.isEmpty()) {
          val (seasons, episodes) = loadSeasonsEpisodes(show, backupShows = backup)
          transactions.withTransaction {
            localSource.seasons.upsert(seasons)
            localSource.episodes.upsert(episodes)
          }
          return@withContext
        }

        val unmatchedEpsBySeason = mutableMapOf<Int, MutableList<BackupUnmatchedEpisode>>()

        for (importEpisode in importEpisodes) {
          val localEpisode =
            localEpisodes
              .firstOrNull {
                it.seasonNumber == importEpisode.seasonNumber && it.episodeNumber == importEpisode.episodeNumber
              }

          if (localEpisode != null) {
            if (!localEpisode.isWatched) {
              episodesManager.setEpisodeWatched(
                showId = IdTmdb(show.tmdbId),
                seasonId = localEpisode.idSeason,
                episodeId = localEpisode.idTmdb,
                customDate = importEpisode.addedAt?.toUtcDateTime(),
              )
            }
          } else {
            unmatchedEpsBySeason
              .getOrPut(importEpisode.seasonNumber) { mutableListOf() }
              .add(
                BackupUnmatchedEpisode(
                  episodeNumber = importEpisode.episodeNumber,
                  seasonNumber = importEpisode.seasonNumber,
                  reason = BackupImportText.of(BackupImportText.Message.EPISODE_NOT_FOUND_ANYWHERE),
                ),
              )
          }
        }

        val localSeasonsAsync = async { localSource.seasons.getAllByShowId(showEntity.idTmdb) }
        val localSeasons = localSeasonsAsync.await()
        val localSeasonNumbers = localSeasons.map { it.seasonNumber }.toSet()

        val backupSeasons = backup.progressSeasons.filter { it.showTmdbId == show.tmdbId }
        val allFailedSeasons = mutableListOf<BackupUnmatchedSeason>()

        for (bs in backupSeasons) {
          if (!localSeasonNumbers.contains(bs.seasonNumber)) {
            val eps = unmatchedEpsBySeason[bs.seasonNumber] ?: emptyList()
            allFailedSeasons +=
              BackupUnmatchedSeason(
                seasonNumber = bs.seasonNumber,
                reason = if (eps.isEmpty()) BackupImportText.of(BackupImportText.Message.SEASON_NOT_FOUND_ANYWHERE) else null,
                unmatchedEpisodes = eps.sortedBy { it.episodeNumber },
              )
          }
        }

        for ((sNum, eps) in unmatchedEpsBySeason) {
          if (allFailedSeasons.none { it.seasonNumber == sNum }) {
            allFailedSeasons +=
              BackupUnmatchedSeason(
                seasonNumber = sNum,
                unmatchedEpisodes = eps.sortedBy { it.episodeNumber },
              )
          }
        }

        if (allFailedSeasons.isNotEmpty()) {
          failedShows +=
            BackupUnmatchedShow(
              title = BackupImportText.verbatim(show.title),
              tmdbId = show.tmdbId,
              unmatchedSeasons = allFailedSeasons.sortedBy { it.seasonNumber },
            )
        }
      }
    }

    private suspend fun loadSeasonsEpisodes(
      show: BackupShow,
      backupShows: BackupShows,
    ): Pair<List<Season>, List<Episode>> =
      coroutineScope {
        val remoteSeasons =
          try {
            remoteSource.tmdb.fetchSeasons(show.tmdbId)
          } catch (error: Throwable) {
            rethrowCancellation(error) {
              Timber.w(error, "Failed to fetch seasons for show ${show.tmdbId}")
            }
            val reason =
              when {
                error is HttpException && error.code() == 404 -> BackupImportText.of(BackupImportText.Message.SEASONS_NOT_FOUND)
                error is HttpException -> BackupImportText.of(BackupImportText.Message.TMDB_API_ERROR, error.code())
                error is IOException -> BackupImportText.of(BackupImportText.Message.TMDB_NETWORK_ERROR)
                else -> BackupImportText.of(BackupImportText.Message.SEASONS_FAILED)
              }
            failedShows +=
              BackupUnmatchedShow(
                title = BackupImportText.verbatim(show.title),
                reason = reason,
                tmdbId = show.tmdbId,
              )
            return@coroutineScope Pair(emptyList(), emptyList())
          }

        val localEpisodesAsync = async { localSource.episodes.getAllWatchedIdsForShows(listOf(show.tmdbId)) }
        val localSeasonsAsync = async { localSource.seasons.getAllWatchedIdsForShows(listOf(show.tmdbId)) }
        val localEpisodesIds = localEpisodesAsync.await()
        val localSeasonsIds = localSeasonsAsync.await()

        val backupSeason = backupShows.progressSeasons.filter { it.showTmdbId == show.tmdbId }
        val backupEpisodes = backupShows.progressEpisodes.filter { it.showTmdbId == show.tmdbId }

        val unmatchedSeasonsList = mutableListOf<BackupUnmatchedSeason>()
        val remoteSeasonNumbers = remoteSeasons.mapNotNull { it.number }.toSet()

        for (bs in backupSeason) {
          if (!remoteSeasonNumbers.contains(bs.seasonNumber)) {
            unmatchedSeasonsList +=
              BackupUnmatchedSeason(
                seasonNumber = bs.seasonNumber,
                reason = BackupImportText.of(BackupImportText.Message.SEASON_NOT_FOUND),
              )
          }
        }

        val episodesBySeason = backupEpisodes.groupBy { it.seasonNumber }
        for ((seasonNum, episodesInSeason) in episodesBySeason) {
          val remoteSeason = remoteSeasons.find { it.number == seasonNum }
          if (remoteSeason == null) {
            if (unmatchedSeasonsList.none { it.seasonNumber == seasonNum }) {
              unmatchedSeasonsList +=
                BackupUnmatchedSeason(
                  seasonNumber = seasonNum,
                  reason = BackupImportText.of(BackupImportText.Message.SEASON_NOT_FOUND),
                )
            }
          } else {
            val remoteEpisodeNumbers =
              remoteSeason.episodes
                ?.mapNotNull { it.number }
                ?.toSet()
                .orEmpty()
            val unmatchedEps = mutableListOf<BackupUnmatchedEpisode>()
            for (be in episodesInSeason) {
              if (!remoteEpisodeNumbers.contains(be.episodeNumber)) {
                unmatchedEps +=
                  BackupUnmatchedEpisode(
                    episodeNumber = be.episodeNumber,
                    seasonNumber = seasonNum,
                    reason = BackupImportText.of(BackupImportText.Message.EPISODE_NOT_FOUND),
                  )
              }
            }
            if (unmatchedEps.isNotEmpty()) {
              unmatchedSeasonsList +=
                BackupUnmatchedSeason(
                  seasonNumber = seasonNum,
                  unmatchedEpisodes = unmatchedEps.sortedBy { it.episodeNumber },
                )
            }
          }
        }

        if (unmatchedSeasonsList.isNotEmpty()) {
          failedShows +=
            BackupUnmatchedShow(
              title = BackupImportText.verbatim(show.title),
              tmdbId = show.tmdbId,
              unmatchedSeasons = unmatchedSeasonsList.sortedBy { it.seasonNumber },
            )
        }

        val seasons =
          remoteSeasons
            .filterNot { localSeasonsIds.contains(it.ids?.tmdb) }
            .map { mappers.season.fromNetwork(it) }
            .map { remoteSeason ->
              val isWatchedNumber =
                backupSeason
                  .any { it.seasonNumber == remoteSeason.number }

              val isWatchedSize =
                backupEpisodes
                  .count { it.seasonNumber == remoteSeason.number } == remoteSeason.episodes.size

              mappers.season.toDatabase(
                season = remoteSeason,
                showId = IdTmdb(show.tmdbId),
                isWatched = isWatchedNumber && isWatchedSize,
              )
            }

        val episodes =
          remoteSeasons.flatMap { season ->
            season.episodes
              ?.filterNot { localEpisodesIds.contains(it.ids?.tmdb) }
              ?.map { episode ->
                val importEpisode =
                  backupEpisodes
                    .find {
                      it.seasonNumber == episode.season &&
                        it.episodeNumber == episode.number
                    }

                val watchedAt = importEpisode?.addedAt?.toUtcDateTime()
                val exportedAt =
                  importEpisode?.let {
                    it.addedAt?.toUtcDateTime() ?: nowUtc()
                  }

                mappers.episode.toDatabase(
                  showId = IdTmdb(show.tmdbId),
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
          val reason =
            when {
              error is HttpException && error.code() == 404 -> BackupImportText.of(BackupImportText.Message.DETAILS_NOT_FOUND)
              error is HttpException -> BackupImportText.of(BackupImportText.Message.TMDB_API_ERROR, error.code())
              error is IOException -> BackupImportText.of(BackupImportText.Message.TMDB_NETWORK_ERROR)
              else -> BackupImportText.of(BackupImportText.Message.DETAILS_FAILED)
            }
          Timber.w("Failed to fetch show: ${show.tmdbId} ${show.title} - $reason")
          failedShows +=
            BackupUnmatchedShow(
              title = BackupImportText.verbatim(show.title),
              reason = reason,
              tmdbId = show.tmdbId,
            )
        }
        false
      }
    }
  }
