package xyz.stignarnia.uiBackup.features.export.runners

import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.dateIsoStringFromMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.ratings.MoviesRatingsRepository
import xyz.stignarnia.uiBackup.model.BackupMovie
import xyz.stignarnia.uiBackup.model.BackupMovieRating
import xyz.stignarnia.uiBackup.model.BackupMovies
import javax.inject.Inject

internal class BackupExportMoviesRunner
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val ratingsRepository: MoviesRatingsRepository,
  ) : BackupExportRunner<BackupMovies>() {
    override suspend fun run(): BackupMovies {
      Timber.d("Initialized.")
      return runExport()
        .also {
          Timber.d("Success.")
        }
    }

    private suspend fun runExport(): BackupMovies =
      withContext(dispatchers.IO) {
        val backupMoviesCollection = exportMoviesCollection()
        val backupMoviesProgress = exportMoviesProgress()
        val backupMoviesRatings = exportMoviesRatings()

        BackupMovies(
          collectionHistory = backupMoviesCollection.collectionHistory,
          collectionWatchlist = backupMoviesCollection.collectionWatchlist,
          collectionHidden = backupMoviesCollection.collectionHidden,
          progressPinned = backupMoviesProgress.progressPinned,
          ratingsMovies = backupMoviesRatings.ratingsMovies,
        )
      }

    private suspend fun exportMoviesCollection(): BackupMovies =
      withContext(dispatchers.IO) {
        val historyMoviesAsync = async { localSource.myMovies.getAll() }
        val watchlistMoviesAsync = async { localSource.watchlistMovies.getAll() }
        val hiddenMoviesAsync = async { localSource.archiveMovies.getAll() }

        val historyMovies = historyMoviesAsync.await()
        val watchlistMovies = watchlistMoviesAsync.await()
        val hiddenMovies = hiddenMoviesAsync.await()

        val collectionHistory =
          historyMovies.map {
            BackupMovie(
              tmdbId = it.idTmdb,
              title = it.title,
              addedAt = dateIsoStringFromMillis(it.updatedAt),
            )
          }
        val collectionWatchlist =
          watchlistMovies.map {
            BackupMovie(
              tmdbId = it.idTmdb,
              title = it.title,
              addedAt = dateIsoStringFromMillis(it.createdAt),
            )
          }
        val collectionHidden =
          hiddenMovies.map {
            BackupMovie(
              tmdbId = it.idTmdb,
              title = it.title,
              addedAt = dateIsoStringFromMillis(it.createdAt),
            )
          }

        BackupMovies(
          collectionHistory = collectionHistory,
          collectionWatchlist = collectionWatchlist,
          collectionHidden = collectionHidden,
        )
      }

    private suspend fun exportMoviesProgress(): BackupMovies =
      withContext(dispatchers.IO) {
        val pinnedMoviesIds = pinnedItemsRepository.getAllMovies()
        BackupMovies(
          progressPinned = pinnedMoviesIds,
        )
      }

    // Ratings

    private suspend fun exportMoviesRatings(): BackupMovies =
      withContext(dispatchers.IO) {
        val ratings = ratingsRepository.loadMoviesRatings()

        val moviesIds = ratings.map { it.idTmdb.id }
        val moviesTmdbIds = localSource.movies.getAllTmdbIds(tmdbIds = moviesIds)

        val ratingsMovies =
          ratings.map {
            BackupMovieRating(
              tmdbId = it.idTmdb.id,
              rating = it.rating,
              ratedAt = dateIsoStringFromMillis(it.ratedAt.toMillis()),
            )
          }

        BackupMovies(
          ratingsMovies = ratingsMovies,
        )
      }
  }
