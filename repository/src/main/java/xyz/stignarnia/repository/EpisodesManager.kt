package xyz.stignarnia.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcZone
import xyz.stignarnia.dataLocal.database.model.EpisodesSyncLog
import xyz.stignarnia.dataLocal.sources.EpisodesLocalDataSource
import xyz.stignarnia.dataLocal.sources.EpisodesSyncLogLocalDataSource
import xyz.stignarnia.dataLocal.sources.SeasonsLocalDataSource
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.SeasonBundle
import xyz.stignarnia.uiModel.Show
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import xyz.stignarnia.dataLocal.database.model.Episode as EpisodeDb
import xyz.stignarnia.dataLocal.database.model.Season as SeasonDb

@Singleton
class EpisodesManager
  @Inject
  constructor(
    private val showsRepository: ShowsRepository,
    private val episodesLocalSource: EpisodesLocalDataSource,
    private val seasonsLocalSource: SeasonsLocalDataSource,
    private val syncLogLocalSource: EpisodesSyncLogLocalDataSource,
    private val transactions: TransactionsProvider,
    private val mappers: Mappers,
  ) {
    suspend fun getWatchedSeasonsIds(show: Show) = seasonsLocalSource.getAllWatchedIdsForShows(listOf(show.tmdbId))

    suspend fun getWatchedEpisodesIds(show: Show) = episodesLocalSource.getAllWatchedIdsForShows(listOf(show.tmdbId))

    suspend fun setSeasonWatched(
      seasonBundle: SeasonBundle,
      customDate: ZonedDateTime?,
    ): List<Episode> {
      val date = customDate?.toUtcZone() ?: nowUtc()
      val toAdd = mutableListOf<EpisodeDb>()
      transactions.withTransaction {
        val (season, show) = seasonBundle

        val dbSeason = mappers.season.toDatabase(season, show.ids.tmdb, true)
        val localSeason = seasonsLocalSource.getById(season.ids.tmdb.id)
        if (localSeason == null) {
          seasonsLocalSource.upsert(listOf(dbSeason))
        }

        val watchedEpisodes = episodesLocalSource.getAllForSeason(season.ids.tmdb.id).filter { it.isWatched }
        season.episodes.forEach { ep ->
          if (watchedEpisodes.none { it.idTmdb == ep.ids.tmdb.id }) {
            val dbEpisode = mappers.episode.toDatabase(ep, season, show.ids.tmdb, true, null, date)
            toAdd.add(dbEpisode)
          }
        }

        episodesLocalSource.upsert(toAdd)
        seasonsLocalSource.update(listOf(dbSeason))
        showsRepository.myShows.updateWatchedAt(show.tmdbId, date.toMillis())
      }
      return toAdd.map { mappers.episode.fromDatabase(it) }
    }

    suspend fun setSeasonUnwatched(seasonBundle: SeasonBundle) {
      transactions.withTransaction {
        val (season, show) = seasonBundle

        val dbSeason = mappers.season.toDatabase(season, show.ids.tmdb, false)
        val watchedEpisodes = episodesLocalSource.getAllForSeason(season.ids.tmdb.id).filter { it.isWatched }
        val toSet = watchedEpisodes.map { it.copy(isWatched = false, lastExportedAt = null, lastWatchedAt = null) }

        val isShowFollowed = showsRepository.myShows.load(show.ids.tmdb) != null

        when {
          isShowFollowed -> {
            episodesLocalSource.upsert(toSet)
            seasonsLocalSource.update(listOf(dbSeason))
          }

          else -> {
            episodesLocalSource.delete(toSet)
            seasonsLocalSource.delete(listOf(dbSeason))
          }
        }
      }
    }

    suspend fun setEpisodeWatched(
      episodeId: Long,
      seasonId: Long,
      showId: IdTmdb,
      customDate: ZonedDateTime?,
    ) {
      val episodeDb = episodesLocalSource.getAllForSeason(seasonId).find { it.idTmdb == episodeId }!!
      val seasonDb = seasonsLocalSource.getById(seasonId)!!
      val show = showsRepository.myShows.load(showId)!!
      setEpisodeWatched(
        episodeBundle =
          EpisodeBundle(
            episode = mappers.episode.fromDatabase(episodeDb),
            season = mappers.season.fromDatabase(seasonDb),
            show = show,
          ),
        customDate = customDate,
      )
    }

    suspend fun setEpisodeWatched(
      episodeBundle: EpisodeBundle,
      customDate: ZonedDateTime?,
    ) {
      transactions.withTransaction {
        val (episode, season, show) = episodeBundle
        val date = customDate?.toUtcZone() ?: nowUtc()

        val dbEpisode = mappers.episode.toDatabase(episode, season, show.ids.tmdb, true, null, date)
        val dbSeason = mappers.season.toDatabase(season, show.ids.tmdb, false)

        val localSeason = seasonsLocalSource.getById(season.ids.tmdb.id)
        if (localSeason == null) {
          seasonsLocalSource.upsert(listOf(dbSeason))
        }
        episodesLocalSource.upsert(listOf(dbEpisode))
        showsRepository.myShows.updateWatchedAt(show.tmdbId, date.toMillis())
        onEpisodeSet(season, show)
      }
    }

    suspend fun setEpisodeUnwatched(episodeBundle: EpisodeBundle) {
      transactions.withTransaction {
        val (episode, season, show) = episodeBundle

        val isShowFollowed = showsRepository.myShows.load(show.ids.tmdb) != null
        val dbEpisode = mappers.episode.toDatabase(episode, season, show.ids.tmdb, true, null, episode.lastWatchedAt)

        when {
          isShowFollowed -> {
            val ep =
              dbEpisode.copy(
                isWatched = false,
                lastExportedAt = null,
                lastWatchedAt = null,
              )
            episodesLocalSource.upsert(listOf(ep))
          }

          else -> {
            episodesLocalSource.delete(listOf(dbEpisode))
          }
        }

        onEpisodeSet(season, show)
      }
    }

    suspend fun setAllUnwatched(
      showId: IdTmdb,
      skipSpecials: Boolean = false,
    ) {
      transactions.withTransaction {
        val watchedEpisodes = episodesLocalSource.getAllByShowId(showId.id)
        val watchedSeasons = seasonsLocalSource.getAllByShowId(showId.id)

        val updateEpisodes =
          watchedEpisodes
            .filter { if (skipSpecials) it.seasonNumber > 0 else true }
            .map { it.copy(isWatched = false, lastExportedAt = null, lastWatchedAt = null) }
        val updateSeasons =
          watchedSeasons
            .filter { if (skipSpecials) it.seasonNumber > 0 else true }
            .map { it.copy(isWatched = false) }

        episodesLocalSource.upsert(updateEpisodes)
        seasonsLocalSource.update(updateSeasons)
      }
    }

    suspend fun invalidateSeasons(
      show: Show,
      remoteSeasons: List<Season>,
    ) {
      if (remoteSeasons.isEmpty()) {
        return
      }
      coroutineScope {
        // Awaited separately rather than through awaitAll, which erases two different element types to List<Any> and needs an unchecked cast to get them back.
        val localSeasonsAsync = async { seasonsLocalSource.getAllByShowId(show.tmdbId) }
        val localEpisodesAsync = async { episodesLocalSource.getAllByShowId(show.tmdbId) }
        val localSeasons = localSeasonsAsync.await()
        val localEpisodes = localEpisodesAsync.await()

        val seasonsToAdd = mutableListOf<SeasonDb>()
        val episodesToAdd = mutableListOf<EpisodeDb>()

        remoteSeasons.forEach { remoteSeason ->
          var isAnyEpisodeUnwatched = false

          remoteSeason.episodes.forEach { remoteEpisode ->
            var localEpisode =
              localEpisodes.find {
                it.episodeNumber == remoteEpisode.number &&
                  it.seasonNumber == remoteEpisode.season
              }
            if (localEpisode == null) {
              // Double check by TMDB ID as season/episode combination might be old.
              localEpisode =
                localEpisodes.find {
                  it.idTmdb == remoteEpisode.ids.tmdb.id
                }
            }

            val isWatched = localEpisode?.isWatched ?: false
            if (!isWatched) {
              isAnyEpisodeUnwatched = true
            }

            val episodeDb =
              mappers.episode.toDatabase(
                episode = remoteEpisode,
                season = remoteSeason,
                showId = show.ids.tmdb,
                isWatched = isWatched,
                lastExportedAt = localEpisode?.lastExportedAt,
                lastWatchedAt = localEpisode?.lastWatchedAt,
              )
            episodesToAdd.add(episodeDb)
          }

          val seasonDb =
            mappers.season.toDatabase(
              season = remoteSeason,
              showId = show.ids.tmdb,
              isWatched = !isAnyEpisodeUnwatched,
            )
          seasonsToAdd.add(seasonDb)
        }

        transactions.withTransaction {
          episodesLocalSource.deleteAllForShow(show.tmdbId)
          seasonsLocalSource.deleteAllForShow(show.tmdbId)

          seasonsLocalSource.upsert(seasonsToAdd)
          episodesLocalSource.upsertChunked(episodesToAdd)

          syncLogLocalSource.upsert(EpisodesSyncLog(show.tmdbId, nowUtcMillis()))
        }

        Timber.d("Episodes updated: ${episodesToAdd.size} Seasons updated: ${seasonsToAdd.size}")
      }
    }

    private suspend fun onEpisodeSet(
      season: Season,
      show: Show,
    ) {
      val localEpisodes = episodesLocalSource.getAllForSeason(season.ids.tmdb.id)
      val isWatched = localEpisodes.count { it.isWatched } == season.episodeCount
      val dbSeason = mappers.season.toDatabase(season, show.ids.tmdb, isWatched)
      seasonsLocalSource.update(listOf(dbSeason))
    }
  }
