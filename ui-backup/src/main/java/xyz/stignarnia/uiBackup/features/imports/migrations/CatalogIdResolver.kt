package xyz.stignarnia.uiBackup.features.imports.migrations

import timber.log.Timber
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import javax.inject.Inject

/**
 * Result of resolving a catalog entry by title against TMDB.
 */
sealed interface CatalogMatchResult {
  data class Matched(val tmdbId: Long) : CatalogMatchResult

  data class Unmatched(val reason: String) : CatalogMatchResult
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
        return CatalogMatchResult.Unmatched("Title in backup is blank.")
      }
      val results =
        try {
          remoteSource.tmdb.fetchSearchResults(trimmed)
        } catch (error: Throwable) {
          rethrowCancellation(error) {
            Timber.w(error, "Failed to look up \"$trimmed\" by title.")
          }
          val message =
            when (error) {
              is retrofit2.HttpException -> "TMDB API error (${error.code()} ${error.message()})."
              is java.io.IOException -> "Network error connecting to TMDB (${error.message ?: "timeout"})."
              else -> error.message ?: error.javaClass.simpleName
            }
          return CatalogMatchResult.Unmatched(message)
        }

      if (results.isEmpty()) {
        return CatalogMatchResult.Unmatched("No results found on TMDB.")
      }

      val shows = results.mapNotNull { it.show }
      if (shows.isEmpty()) {
        return CatalogMatchResult.Unmatched("Only movies found on TMDB, no TV shows.")
      }

      val exactMatch = shows.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
      if (exactMatch == null) {
        val foundTitles =
          shows
            .map { it.title }
            .distinct()
            .take(2)
            .joinToString(", ") { "\"$it\"" }
        return CatalogMatchResult.Unmatched("No exact title match on TMDB (found: $foundTitles).")
      }

      val tmdbId = exactMatch.ids?.tmdb
      if (tmdbId == null || tmdbId <= 0) {
        return CatalogMatchResult.Unmatched("TMDB entry has missing or invalid ID ($tmdbId).")
      }

      return CatalogMatchResult.Matched(tmdbId)
    }

    override suspend fun findMovieByTitle(title: String): CatalogMatchResult {
      val trimmed = title.trim()
      if (trimmed.isEmpty()) {
        return CatalogMatchResult.Unmatched("Title in backup is blank.")
      }
      val results =
        try {
          remoteSource.tmdb.fetchSearchResults(trimmed)
        } catch (error: Throwable) {
          rethrowCancellation(error) {
            Timber.w(error, "Failed to look up \"$trimmed\" by title.")
          }
          val message =
            when (error) {
              is retrofit2.HttpException -> "TMDB API error (${error.code()} ${error.message()})."
              is java.io.IOException -> "Network error connecting to TMDB (${error.message ?: "timeout"})."
              else -> error.message ?: error.javaClass.simpleName
            }
          return CatalogMatchResult.Unmatched(message)
        }

      if (results.isEmpty()) {
        return CatalogMatchResult.Unmatched("No results found on TMDB.")
      }

      val movies = results.mapNotNull { it.movie }
      if (movies.isEmpty()) {
        return CatalogMatchResult.Unmatched("Only TV shows found on TMDB, no movies.")
      }

      val exactMatch = movies.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
      if (exactMatch == null) {
        val foundTitles =
          movies
            .map { it.title }
            .distinct()
            .take(2)
            .joinToString(", ") { "\"$it\"" }
        return CatalogMatchResult.Unmatched("No exact title match on TMDB (found: $foundTitles).")
      }

      val tmdbId = exactMatch.ids?.tmdb
      if (tmdbId == null || tmdbId <= 0) {
        return CatalogMatchResult.Unmatched("TMDB entry has missing or invalid ID ($tmdbId).")
      }

      return CatalogMatchResult.Matched(tmdbId)
    }
  }
