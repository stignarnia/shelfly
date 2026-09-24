package xyz.stignarnia.uiBackup.features.imports.migrations

import retrofit2.HttpException
import timber.log.Timber
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportText
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import java.io.IOException
import javax.inject.Inject

/**
 * Result of resolving a catalog entry by title against TMDB.
 */
sealed interface CatalogMatchResult {
  data class Matched(val tmdbId: Long) : CatalogMatchResult

  data class Unmatched(val reason: BackupImportText) : CatalogMatchResult
}

/**
 * Last resort for backup entries that carry no TMDB id at all.
 * Only an exact title match counts.
 * A backup entry has nothing else to match on - no year, no external ids - so anything looser would quietly attach somebody else's watch history to the wrong show.
 */
interface CatalogIdResolver {
  suspend fun findShowByTitle(title: String): CatalogMatchResult

  suspend fun findMovieByTitle(title: String): CatalogMatchResult
}

internal class TmdbCatalogIdResolver
  @Inject
  constructor(
    private val remoteSource: RemoteDataSource,
  ) : CatalogIdResolver {
    override suspend fun findShowByTitle(title: String): CatalogMatchResult {
      val trimmed = title.trim()
      if (trimmed.isEmpty()) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.TITLE_BLANK))
      }
      val results =
        try {
          remoteSource.tmdb.fetchSearchResults(trimmed)
        } catch (error: Throwable) {
          rethrowCancellation(error) {
            Timber.w(error, "Failed to look up \"$trimmed\" by title.")
          }
          return CatalogMatchResult.Unmatched(lookupFailure(error))
        }

      if (results.isEmpty()) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.NO_RESULTS))
      }

      val shows = results.mapNotNull { it.show }
      if (shows.isEmpty()) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.ONLY_MOVIES_FOUND))
      }

      val exactMatch = shows.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
      if (exactMatch == null) {
        val foundTitles =
          shows
            .map { it.title }
            .distinct()
            .take(2)
            .joinToString(", ") { "\"$it\"" }
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.NO_EXACT_MATCH, foundTitles))
      }

      val tmdbId = exactMatch.ids?.tmdb
      if (tmdbId == null || tmdbId <= 0) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.INVALID_TMDB_ID))
      }

      return CatalogMatchResult.Matched(tmdbId)
    }

    override suspend fun findMovieByTitle(title: String): CatalogMatchResult {
      val trimmed = title.trim()
      if (trimmed.isEmpty()) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.TITLE_BLANK))
      }
      val results =
        try {
          remoteSource.tmdb.fetchSearchResults(trimmed)
        } catch (error: Throwable) {
          rethrowCancellation(error) {
            Timber.w(error, "Failed to look up \"$trimmed\" by title.")
          }
          return CatalogMatchResult.Unmatched(lookupFailure(error))
        }

      if (results.isEmpty()) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.NO_RESULTS))
      }

      val movies = results.mapNotNull { it.movie }
      if (movies.isEmpty()) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.ONLY_SHOWS_FOUND))
      }

      val exactMatch = movies.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
      if (exactMatch == null) {
        val foundTitles =
          movies
            .map { it.title }
            .distinct()
            .take(2)
            .joinToString(", ") { "\"$it\"" }
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.NO_EXACT_MATCH, foundTitles))
      }

      val tmdbId = exactMatch.ids?.tmdb
      if (tmdbId == null || tmdbId <= 0) {
        return CatalogMatchResult.Unmatched(BackupImportText.of(BackupImportText.Message.INVALID_TMDB_ID))
      }

      return CatalogMatchResult.Matched(tmdbId)
    }

    private fun lookupFailure(error: Throwable) =
      when (error) {
        is HttpException -> BackupImportText.of(BackupImportText.Message.TMDB_API_ERROR, error.code())
        is IOException -> BackupImportText.of(BackupImportText.Message.TMDB_NETWORK_ERROR)
        else -> BackupImportText.of(BackupImportText.Message.LOOKUP_FAILED)
      }
  }
