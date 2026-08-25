package xyz.stignarnia.ui_backup.features.import_.migrations

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.ui_backup.BackupConfig.SCHEME_VERSION
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupListsV2
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupMovieV2
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupMoviesV2
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupSchemeV2
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupShowV2
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupShowsV2
import xyz.stignarnia.ui_backup.model.BackupEpisode
import xyz.stignarnia.ui_backup.model.BackupEpisodeRating
import xyz.stignarnia.ui_backup.model.BackupList
import xyz.stignarnia.ui_backup.model.BackupListItem
import xyz.stignarnia.ui_backup.model.BackupLists
import xyz.stignarnia.ui_backup.model.BackupMovie
import xyz.stignarnia.ui_backup.model.BackupMovieRating
import xyz.stignarnia.ui_backup.model.BackupMovies
import xyz.stignarnia.ui_backup.model.BackupScheme
import xyz.stignarnia.ui_backup.model.BackupSeason
import xyz.stignarnia.ui_backup.model.BackupSeasonRating
import xyz.stignarnia.ui_backup.model.BackupShow
import xyz.stignarnia.ui_backup.model.BackupShowRating
import xyz.stignarnia.ui_backup.model.BackupShows
import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Reads a v2 backup - the scheme Showly wrote before this fork - and re-keys it onto TMDB ids.
 *
 * v2 identifies everything by ids from the old catalog source, which no longer resolve to anything.
 * The remap is therefore driven by the show and movie collections, the only place where an old id and a TMDB id sit side by side: every child entry (episode, season, rating, list item) finds its parent by old id and inherits the parent's TMDB id.
 *
 * Entries whose parent cannot be found are dropped and counted in [BackupMigrationReport], never guessed at.
 */
class BackupMigrationV2 @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val resolver: CatalogIdResolver,
) {

  companion object {
    const val VERSION = 2
  }

  suspend fun migrate(jsonInput: String): BackupMigrationResult =
    withContext(dispatchers.IO) {
      val moshi = Moshi
        .Builder()
        .build()

      val scheme = moshi
        .adapter(BackupSchemeV2::class.java)
        .fromJson(jsonInput)
        ?: throw IllegalArgumentException("Backup file is empty.")

      migrate(scheme)
    }

  internal suspend fun migrate(scheme: BackupSchemeV2): BackupMigrationResult {
    val showIds = resolveShowIds(scheme.shows)
    val movieIds = resolveMovieIds(scheme.movies)

    val report = ReportBuilder(
      unmatchedShows = showIds.unmatched,
      unmatchedMovies = movieIds.unmatched,
    )

    val migrated = BackupScheme(
      version = SCHEME_VERSION,
      platform = scheme.platform,
      createdAt = scheme.createdAt,
      shows = migrateShows(scheme.shows, showIds.byLegacyId, report),
      movies = migrateMovies(scheme.movies, movieIds.byLegacyId, report),
      lists = migrateLists(scheme.lists, showIds.byLegacyId, movieIds.byLegacyId, report),
    )

    return BackupMigrationResult(
      scheme = migrated,
      report = report.build(),
    ).also {
      Timber.d("Migrated v${scheme.version} backup: ${it.report}")
    }
  }

  // Collections are the only source of truth for the old id -> TMDB id mapping.

  private suspend fun resolveShowIds(shows: BackupShowsV2): ResolvedIds {
    val entries = shows.collectionHistory + shows.collectionWatchlist + shows.collectionHidden
    return resolveIds(
      entries = entries.map { it.legacyId to it },
      tmdbIdOf = { it.tmdbId },
      titleOf = { it.title },
      findByTitle = resolver::findShowByTitle,
    )
  }

  private suspend fun resolveMovieIds(movies: BackupMoviesV2): ResolvedIds {
    val entries = movies.collectionHistory + movies.collectionWatchlist + movies.collectionHidden
    return resolveIds(
      entries = entries.map { it.legacyId to it },
      tmdbIdOf = { it.tmdbId },
      titleOf = { it.title },
      findByTitle = resolver::findMovieByTitle,
    )
  }

  private suspend fun <T> resolveIds(
    entries: List<Pair<Long, T>>,
    tmdbIdOf: (T) -> Long,
    titleOf: (T) -> String,
    findByTitle: suspend (String) -> Long?,
  ): ResolvedIds {
    val byLegacyId = mutableMapOf<Long, Long>()
    val unmatched = mutableListOf<String>()

    for ((legacyId, entry) in entries) {
      if (byLegacyId.containsKey(legacyId)) {
        continue
      }
      val title = titleOf(entry)
      val tmdbId = tmdbIdOf(entry).takeIf { it > 0 } ?: findByTitle(title)
      if (tmdbId != null && tmdbId > 0) {
        byLegacyId[legacyId] = tmdbId
      } else {
        Timber.w("No TMDB id for \"$title\". Dropping it and everything under it.")
        unmatched += title
      }
    }

    return ResolvedIds(byLegacyId, unmatched)
  }

  private fun migrateShows(
    shows: BackupShowsV2,
    showIds: Map<Long, Long>,
    report: ReportBuilder,
  ): BackupShows {
    fun List<BackupShowV2>.migrate() =
      mapNotNull { show ->
        showIds[show.legacyId]?.let {
          BackupShow(
            tmdbId = it,
            title = show.title,
            addedAt = show.addedAt,
            updatedAt = show.updatedAt,
          )
        }
      }

    return BackupShows(
      collectionHistory = shows.collectionHistory.migrate(),
      collectionWatchlist = shows.collectionWatchlist.migrate(),
      collectionHidden = shows.collectionHidden.migrate(),
      progressSeasons = shows.progressSeasons.mapNotNull { season ->
        val showTmdbId = showIds[season.showLegacyId] ?: season.showTmdbId.takeIf { it > 0 }
        if (showTmdbId == null) {
          report.skippedSeasons++
          return@mapNotNull null
        }
        BackupSeason(
          showTmdbId = showTmdbId,
          seasonNumber = season.seasonNumber,
        )
      },
      progressEpisodes = shows.progressEpisodes.mapNotNull { episode ->
        val showTmdbId = showIds[episode.showLegacyId]
        if (showTmdbId == null) {
          report.skippedEpisodes++
          return@mapNotNull null
        }
        BackupEpisode(
          showTmdbId = showTmdbId,
          seasonNumber = episode.seasonNumber,
          episodeNumber = episode.episodeNumber,
          addedAt = episode.addedAt,
        )
      },
      progressPinned = shows.progressPinned.mapNotNull { showIds[it] },
      progressOnHold = shows.progressOnHold.mapNotNull { showIds[it] },
      ratingsShows = shows.ratingsShows.mapNotNull { rating ->
        val showTmdbId = showIds[rating.showLegacyId] ?: rating.showTmdbId.takeIf { it > 0 }
        if (showTmdbId == null) {
          report.skippedShowRatings++
          return@mapNotNull null
        }
        BackupShowRating(
          tmdbId = showTmdbId,
          rating = rating.rating,
          ratedAt = rating.ratedAt,
        )
      },
      ratingsSeasons = shows.ratingsSeasons.mapNotNull { rating ->
        val showTmdbId = showIds[rating.showLegacyId] ?: rating.showTmdbId.takeIf { it > 0 }
        if (showTmdbId == null) {
          report.skippedSeasonRatings++
          return@mapNotNull null
        }
        BackupSeasonRating(
          showTmdbId = showTmdbId,
          seasonNumber = rating.seasonNumber,
          rating = rating.rating,
          ratedAt = rating.ratedAt,
        )
      },
      ratingsEpisodes = shows.ratingsEpisodes.mapNotNull { rating ->
        val showTmdbId = showIds[rating.showLegacyId]
        if (showTmdbId == null) {
          report.skippedEpisodeRatings++
          return@mapNotNull null
        }
        BackupEpisodeRating(
          showTmdbId = showTmdbId,
          seasonNumber = rating.seasonNumber,
          episodeNumber = rating.episodeNumber,
          rating = rating.rating,
          ratedAt = rating.ratedAt,
        )
      },
    )
  }

  private fun migrateMovies(
    movies: BackupMoviesV2,
    movieIds: Map<Long, Long>,
    report: ReportBuilder,
  ): BackupMovies {
    fun List<BackupMovieV2>.migrate() =
      mapNotNull { movie ->
        movieIds[movie.legacyId]?.let {
          BackupMovie(
            tmdbId = it,
            title = movie.title,
            addedAt = movie.addedAt,
          )
        }
      }

    return BackupMovies(
      collectionHistory = movies.collectionHistory.migrate(),
      collectionWatchlist = movies.collectionWatchlist.migrate(),
      collectionHidden = movies.collectionHidden.migrate(),
      progressPinned = movies.progressPinned.mapNotNull { movieIds[it] },
      ratingsMovies = movies.ratingsMovies.mapNotNull { rating ->
        val movieTmdbId = movieIds[rating.movieLegacyId] ?: rating.movieTmdbId.takeIf { it > 0 }
        if (movieTmdbId == null) {
          report.skippedMovieRatings++
          return@mapNotNull null
        }
        BackupMovieRating(
          tmdbId = movieTmdbId,
          rating = rating.rating,
          ratedAt = rating.ratedAt,
        )
      },
    )
  }

  private fun migrateLists(
    lists: BackupListsV2,
    showIds: Map<Long, Long>,
    movieIds: Map<Long, Long>,
    report: ReportBuilder,
  ): BackupLists =
    BackupLists(
      lists = lists.lists.map { list ->
        BackupList(
          id = list.id,
          slugId = list.slugId,
          name = list.name,
          description = list.description,
          privacy = list.privacy,
          itemCount = list.itemCount,
          createdAt = list.createdAt,
          updatedAt = list.updatedAt,
          items = list.items.mapNotNull { item ->
            val tmdbId = item.tmdbId.takeIf { it > 0 }
              ?: when (item.type) {
                "show" -> showIds[item.legacyId]
                "movie" -> movieIds[item.legacyId]
                else -> null
              }
            if (tmdbId == null) {
              report.skippedListItems++
              return@mapNotNull null
            }
            BackupListItem(
              id = item.id,
              listId = item.listId,
              tmdbId = tmdbId,
              type = item.type,
              rank = item.rank,
              listedAt = item.listedAt,
              createdAt = item.createdAt,
              updatedAt = item.updatedAt,
            )
          },
        )
      },
    )

  private data class ResolvedIds(
    val byLegacyId: Map<Long, Long>,
    val unmatched: List<String>,
  )

  private class ReportBuilder(
    val unmatchedShows: List<String>,
    val unmatchedMovies: List<String>,
  ) {
    var skippedSeasons = 0
    var skippedEpisodes = 0
    var skippedShowRatings = 0
    var skippedSeasonRatings = 0
    var skippedEpisodeRatings = 0
    var skippedMovieRatings = 0
    var skippedListItems = 0

    fun build() =
      BackupMigrationReport(
        unmatchedShows = unmatchedShows,
        unmatchedMovies = unmatchedMovies,
        skippedSeasons = skippedSeasons,
        skippedEpisodes = skippedEpisodes,
        skippedShowRatings = skippedShowRatings,
        skippedSeasonRatings = skippedSeasonRatings,
        skippedEpisodeRatings = skippedEpisodeRatings,
        skippedMovieRatings = skippedMovieRatings,
        skippedListItems = skippedListItems,
      )
  }
}
