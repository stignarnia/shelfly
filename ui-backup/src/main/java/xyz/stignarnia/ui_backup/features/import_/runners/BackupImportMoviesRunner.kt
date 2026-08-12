package xyz.stignarnia.ui_backup.features.import_.runners

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcDateTime
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.ArchiveMovie
import xyz.stignarnia.data_local.database.model.MyMovie
import xyz.stignarnia.data_local.database.model.Rating
import xyz.stignarnia.data_local.database.model.WatchlistMovie
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.movies.ratings.MoviesRatingsRepository
import xyz.stignarnia.ui_backup.features.import_.model.BackupImportStatus.Importing
import xyz.stignarnia.ui_backup.model.BackupMovie
import xyz.stignarnia.ui_backup.model.BackupMovies
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_model.IdTmdb
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

internal class BackupImportMoviesRunner @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val moviesRepository: MoviesRepository,
  private val ratingsRepository: MoviesRatingsRepository,
  private val pinnedItemsRepository: PinnedItemsRepository,
) : BackupImportRunner<BackupMovies>() {

  override suspend fun run(backup: BackupMovies) {
    Timber.d("Initialized.")
    runImport(backup)
      .also {
        Timber.d("Success.")
      }
  }

  private suspend fun runImport(backup: BackupMovies) {
    withContext(dispatchers.IO) {
      importMoviesCollection(backup)
      importMoviesPinned(backup)
      importMoviesRatings(backup)
    }
  }

  private suspend fun importMoviesPinned(backup: BackupMovies) {
    withContext(dispatchers.IO) {
      val localPinned = pinnedItemsRepository.getAllMovies()
      for (pinned in backup.progressPinned) {
        if (!localPinned.contains(pinned)) {
          pinnedItemsRepository.addMoviePinnedItem(IdTmdb(pinned))
        }
      }
    }
  }

  private suspend fun importMoviesRatings(backup: BackupMovies) {
    withContext(dispatchers.IO) {
      val localRatings = ratingsRepository.loadMoviesRatings()

      for (rating in backup.ratingsMovies) {
        if (localRatings.any { it.idTmdb.id == rating.tmdbId }) {
          continue
        }

        val entity = Rating(
          idTmdb = rating.tmdbId,
          type = "movie",
          rating = rating.rating,
          seasonNumber = null,
          episodeNumber = null,
          ratedAt = rating.ratedAt.toUtcDateTime() ?: nowUtc(),
          createdAt = nowUtc(),
          updatedAt = nowUtc(),
        )

        localSource.ratings.replace(entity)
      }
    }
  }

  private suspend fun importMoviesCollection(backup: BackupMovies) {
    withContext(dispatchers.IO) {
      val localCollection = moviesRepository
        .loadCollection()
        .map { it.tmdbId }

      importMyMovies(backup, localCollection)
      importWatchlistMovies(backup, localCollection)
      importHiddenMovies(backup, localCollection)
    }
  }

  private suspend fun importMyMovies(
    backupMovies: BackupMovies,
    localCollection: List<Long>,
  ) {
    for (movie in backupMovies.collectionHistory) {
      Timber.d("Importing movie ${movie.tmdbId} ...")
      statusListener?.invoke(Importing(movie.title))

      if (localCollection.contains(movie.tmdbId)) {
        Timber.d("Movie already in collection. Skipping.")
        continue
      }

      val movieDetails = localSource.movies.getById(movie.tmdbId)
      if (movieDetails == null) {
        if (!fetchMovieDetails(movie)) {
          continue
        }
      }

      val timestamp = movie.addedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
      val myMovie = MyMovie.fromTmdbId(movie.tmdbId, timestamp)
      localSource.myMovies.insert(listOf(myMovie))

      Timber.d("Added to history ${movie.tmdbId} ...")
    }
  }

  private suspend fun importWatchlistMovies(
    backupMovies: BackupMovies,
    localCollection: List<Long>,
  ) {
    for (movie in backupMovies.collectionWatchlist) {
      Timber.d("Importing movie ${movie.tmdbId} ...")
      statusListener?.invoke(Importing(movie.title))

      if (localCollection.contains(movie.tmdbId)) {
        Timber.d("Movie already in collection. Skipping.")
        continue
      }

      val movieDetails = localSource.movies.getById(movie.tmdbId)
      if (movieDetails == null) {
        if (!fetchMovieDetails(movie)) {
          continue
        }
      }

      val timestamp = movie.addedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
      val watchlistMovie = WatchlistMovie.fromTmdbId(movie.tmdbId, timestamp)
      localSource.watchlistMovies.insert(watchlistMovie)

      Timber.d("Added to Watchlist ${movie.tmdbId} ...")
    }
  }

  private suspend fun importHiddenMovies(
    backupMovies: BackupMovies,
    localCollection: List<Long>,
  ) {
    for (movie in backupMovies.collectionHidden) {
      Timber.d("Importing movie ${movie.tmdbId} ...")
      statusListener?.invoke(Importing(movie.title))

      if (localCollection.contains(movie.tmdbId)) {
        Timber.d("Movie already in collection. Skipping.")
        continue
      }

      val movieDetails = localSource.movies.getById(movie.tmdbId)
      if (movieDetails == null) {
        if (!fetchMovieDetails(movie)) {
          continue
        }
      }

      val timestamp = movie.addedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis()
      val hiddenMovie = ArchiveMovie.fromTmdbId(movie.tmdbId, timestamp)
      localSource.archiveMovies.insert(hiddenMovie)

      Timber.d("Added to Hidden ${movie.tmdbId} ...")
    }
  }

  private suspend fun fetchMovieDetails(movie: BackupMovie): Boolean {
    Timber.d("Fetching remote movie details for ${movie.tmdbId} ...")
    return try {
      moviesRepository.movieDetails.load(IdTmdb(movie.tmdbId), force = true)
      true
    } catch (error: Throwable) {
      rethrowCancellation(error) {
        if (error is HttpException && error.code() == 404) {
          Timber.w("Failed to fetch movie: ${movie.tmdbId} ${movie.title}")
        }
      }
      false
    }
  }
}
