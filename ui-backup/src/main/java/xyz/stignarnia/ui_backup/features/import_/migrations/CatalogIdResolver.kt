package xyz.stignarnia.ui_backup.features.import_.migrations

import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.data_remote.catalog.model.SearchResult
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import timber.log.Timber
import javax.inject.Inject

/**
 * Last resort for backup entries that carry no TMDB id at all.
 *
 * Only an exact title match counts.
 * A backup entry has nothing else to match on
 * - no year, no external ids - so anything looser would quietly attach somebody else's watch history to the wrong show.
 */
interface CatalogIdResolver {

  suspend fun findShowByTitle(title: String): Long?

  suspend fun findMovieByTitle(title: String): Long?
}

internal class TmdbCatalogIdResolver @Inject constructor(
  private val remoteSource: RemoteDataSource,
) : CatalogIdResolver {

  override suspend fun findShowByTitle(title: String): Long? =
    search(title).firstNotNullOfOrNull { result ->
      result.show
        ?.takeIf { it.title.equals(title, ignoreCase = true) }
        ?.ids
        ?.tmdb
        ?.takeIf { it > 0 }
    }

  override suspend fun findMovieByTitle(title: String): Long? =
    search(title).firstNotNullOfOrNull { result ->
      result.movie
        ?.takeIf { it.title.equals(title, ignoreCase = true) }
        ?.ids
        ?.tmdb
        ?.takeIf { it > 0 }
    }

  private suspend fun search(title: String): List<SearchResult> =
    try {
      remoteSource.tmdb.fetchSearchResults(title)
    } catch (error: Throwable) {
      rethrowCancellation(error) {
        Timber.w(error, "Failed to look up \"$title\" by title.")
      }
      emptyList()
    }
}
