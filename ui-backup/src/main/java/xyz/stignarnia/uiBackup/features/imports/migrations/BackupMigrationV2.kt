package xyz.stignarnia.uiBackup.features.imports.migrations

import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.uiBackup.BackupConfig.SCHEME_VERSION
import xyz.stignarnia.uiBackup.features.imports.migrations.model.BackupListItemV2
import xyz.stignarnia.uiBackup.features.imports.migrations.model.BackupListsV2
import xyz.stignarnia.uiBackup.features.imports.migrations.model.BackupMovieV2
import xyz.stignarnia.uiBackup.features.imports.migrations.model.BackupMoviesV2
import xyz.stignarnia.uiBackup.features.imports.migrations.model.BackupSchemeV2
import xyz.stignarnia.uiBackup.features.imports.migrations.model.BackupShowV2
import xyz.stignarnia.uiBackup.features.imports.migrations.model.BackupShowsV2
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedEpisode
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedItem
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedList
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedSeason
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedShow
import xyz.stignarnia.uiBackup.model.BackupEpisode
import xyz.stignarnia.uiBackup.model.BackupEpisodeRating
import xyz.stignarnia.uiBackup.model.BackupList
import xyz.stignarnia.uiBackup.model.BackupListItem
import xyz.stignarnia.uiBackup.model.BackupLists
import xyz.stignarnia.uiBackup.model.BackupMovie
import xyz.stignarnia.uiBackup.model.BackupMovieRating
import xyz.stignarnia.uiBackup.model.BackupMovies
import xyz.stignarnia.uiBackup.model.BackupScheme
import xyz.stignarnia.uiBackup.model.BackupSeason
import xyz.stignarnia.uiBackup.model.BackupSeasonRating
import xyz.stignarnia.uiBackup.model.BackupShow
import xyz.stignarnia.uiBackup.model.BackupShowRating
import xyz.stignarnia.uiBackup.model.BackupShows
import javax.inject.Inject

/**
 * Reads a v2 backup - the scheme Showly wrote before this fork - and re-keys it onto TMDB ids.
 *
 * v2 identifies everything by ids from the old catalog source, which no longer resolve to anything.
 * The remap is therefore driven by the show and movie collections, the only place where an old id and a TMDB id sit side by side: every child entry (episode, season, rating, list item) finds its parent by old id and inherits the parent's TMDB id.
 *
 * Entries whose parent cannot be found are dropped and counted in [BackupMigrationReport], never guessed at.
 */
class BackupMigrationV2
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val resolver: CatalogIdResolver,
  ) {
    companion object {
      const val VERSION = 2
    }

    suspend fun migrate(jsonInput: String): BackupMigrationResult =
      withContext(dispatchers.IO) {
        val moshi =
          Moshi
            .Builder()
            .build()

        val scheme =
          moshi
            .adapter(BackupSchemeV2::class.java)
            .fromJson(jsonInput)
            ?: throw IllegalArgumentException("Backup file is empty.")

        migrate(scheme)
      }

    internal suspend fun migrate(scheme: BackupSchemeV2): BackupMigrationResult {
      val showIds = resolveShowIds(scheme.shows)
      val movieIds = resolveMovieIds(scheme.movies)

      val knownShowLegacyIds =
        (scheme.shows.collectionHistory + scheme.shows.collectionWatchlist + scheme.shows.collectionHidden)
          .map { it.legacyId }
          .toSet()

      val orphanShowLegacyIds =
        (scheme.shows.progressEpisodes.map { it.showLegacyId } + scheme.shows.progressSeasons.map { it.showLegacyId })
          .filterNot { knownShowLegacyIds.contains(it) || it <= 0 }
          .distinct()

      val orphanEntries =
        orphanShowLegacyIds.map { orphanId ->
          UnmatchedLegacyEntry(
            legacyId = orphanId,
            title = "Serie TV non archiviata (ID: $orphanId)",
            reason = "Serie TV presente nella cronologia ma non nel catalogo TMDB.",
          )
        }

      val allUnmatchedShowEntries = showIds.unmatched + orphanEntries

      val unmatchedShowsList =
        allUnmatchedShowEntries.map { entry ->
          val episodesForShow = scheme.shows.progressEpisodes.filter { it.showLegacyId == entry.legacyId }
          val seasonsForShow = scheme.shows.progressSeasons.filter { it.showLegacyId == entry.legacyId }

          val epsBySeason = episodesForShow.groupBy { it.seasonNumber }
          val allSeasonNums = (seasonsForShow.map { it.seasonNumber } + epsBySeason.keys).distinct().sorted()

          val seasonsList =
            allSeasonNums.map { sNum ->
              val episodes =
                (epsBySeason[sNum] ?: emptyList()).map { ep ->
                  BackupUnmatchedEpisode(
                    episodeNumber = ep.episodeNumber,
                    seasonNumber = sNum,
                    reason = "Serie TV non disponibile su TMDB.",
                  )
                }
              BackupUnmatchedSeason(
                seasonNumber = sNum,
                reason = if (episodes.isEmpty()) "Stagione non disponibile su TMDB." else null,
                unmatchedEpisodes = episodes.sortedBy { it.episodeNumber },
              )
            }

          BackupUnmatchedShow(
            title = entry.title,
            reason = entry.reason,
            tmdbId = null,
            unmatchedSeasons = seasonsList,
          )
        }

      val unmatchedMoviesList =
        movieIds.unmatched.map {
          BackupUnmatchedItem(title = it.title, reason = it.reason)
        }

      val report =
        ReportBuilder(
          unmatchedShows = unmatchedShowsList,
          unmatchedMovies = unmatchedMoviesList,
        )

      val migrated =
        BackupScheme(
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
      findByTitle: suspend (String) -> CatalogMatchResult,
    ): ResolvedIds {
      val byLegacyId = mutableMapOf<Long, Long>()
      val unmatched = mutableListOf<UnmatchedLegacyEntry>()
      val seenLegacyIds = mutableSetOf<Long>()

      for ((legacyId, entry) in entries) {
        if (!seenLegacyIds.add(legacyId)) {
          continue
        }
        val title = titleOf(entry)
        val tmdbId = tmdbIdOf(entry).takeIf { it > 0 }
        if (tmdbId != null) {
          byLegacyId[legacyId] = tmdbId
        } else {
          when (val result = findByTitle(title)) {
            is CatalogMatchResult.Matched -> {
              byLegacyId[legacyId] = result.tmdbId
            }

            is CatalogMatchResult.Unmatched -> {
              Timber.w("No TMDB id for \"$title\". Dropping it: ${result.reason}")
              unmatched += UnmatchedLegacyEntry(legacyId = legacyId, title = title, reason = result.reason)
            }
          }
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
        progressSeasons =
          shows.progressSeasons.mapNotNull { season ->
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
        progressEpisodes =
          shows.progressEpisodes.mapNotNull { episode ->
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
        ratingsShows =
          shows.ratingsShows.mapNotNull { rating ->
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
        ratingsSeasons =
          shows.ratingsSeasons.mapNotNull { rating ->
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
        ratingsEpisodes =
          shows.ratingsEpisodes.mapNotNull { rating ->
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
        ratingsMovies =
          movies.ratingsMovies.mapNotNull { rating ->
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
        lists =
          lists.lists.map { list ->
            val unmatchedItems = mutableListOf<BackupUnmatchedItem>()
            val items =
              list.items.mapNotNull { item ->
                val tmdbId =
                  item.tmdbId.takeIf { it > 0 }
                    ?: when (item.type) {
                      "show" -> showIds[item.legacyId]
                      "movie" -> movieIds[item.legacyId]
                      else -> null
                    }
                if (tmdbId == null) {
                  unmatchedItems +=
                    BackupUnmatchedItem(
                      title = unmatchedListItemTitle(item),
                      reason = "Elemento senza ID TMDB e non presente nella collezione.",
                    )
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
              }
            if (unmatchedItems.isNotEmpty()) {
              report.unmatchedLists += BackupUnmatchedList(title = list.name, unmatchedItems = unmatchedItems)
            }
            BackupList(
              id = list.id,
              slugId = list.slugId,
              name = list.name,
              description = list.description,
              privacy = list.privacy,
              itemCount = list.itemCount,
              createdAt = list.createdAt,
              updatedAt = list.updatedAt,
              items = items,
            )
          },
      )

    // A v2 list item carries no title, and the collection entry that would have named it is the thing that is missing.
    private fun unmatchedListItemTitle(item: BackupListItemV2) =
      when (item.type) {
        "show" -> "Serie TV non archiviata (ID: ${item.legacyId})"
        "movie" -> "Film non archiviato (ID: ${item.legacyId})"
        else -> "Elemento non archiviato (ID: ${item.legacyId})"
      }

    private data class UnmatchedLegacyEntry(
      val legacyId: Long,
      val title: String,
      val reason: String,
    )

    private data class ResolvedIds(
      val byLegacyId: Map<Long, Long>,
      val unmatched: List<UnmatchedLegacyEntry>,
    )

    private class ReportBuilder(
      val unmatchedShows: List<BackupUnmatchedShow>,
      val unmatchedMovies: List<BackupUnmatchedItem>,
    ) {
      var skippedSeasons = 0
      var skippedEpisodes = 0
      var skippedShowRatings = 0
      var skippedSeasonRatings = 0
      var skippedEpisodeRatings = 0
      var skippedMovieRatings = 0
      val unmatchedLists = mutableListOf<BackupUnmatchedList>()

      fun build() =
        BackupMigrationReport(
          unmatchedShows = unmatchedShows,
          unmatchedMovies = unmatchedMovies,
          unmatchedLists = unmatchedLists.toList(),
          skippedSeasons = skippedSeasons,
          skippedEpisodes = skippedEpisodes,
          skippedShowRatings = skippedShowRatings,
          skippedSeasonRatings = skippedSeasonRatings,
          skippedEpisodeRatings = skippedEpisodeRatings,
          skippedMovieRatings = skippedMovieRatings,
        )
    }
  }
