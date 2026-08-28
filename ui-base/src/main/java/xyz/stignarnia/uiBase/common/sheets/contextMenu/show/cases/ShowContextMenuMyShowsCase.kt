package xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject
import xyz.stignarnia.dataLocal.database.model.Episode as EpisodeDb
import xyz.stignarnia.dataLocal.database.model.Season as SeasonDb

@ViewModelScoped
class ShowContextMenuMyShowsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val transactions: TransactionsProvider,
    private val remoteSource: RemoteDataSource,
    private val mappers: Mappers,
    private val showsRepository: ShowsRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val settingsRepository: SettingsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun moveToMyShows(tmdbId: IdTmdb) =
      withContext(dispatchers.IO) {
        val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))

        val (isWatchlist, isHidden) =
          awaitAll(
            async { showsRepository.watchlistShows.exists(tmdbId) },
            async { showsRepository.hiddenShows.exists(tmdbId) },
          )

        val seasons =
          remoteSource.tmdb
            .fetchSeasons(tmdbId.id)
            .map { mappers.season.fromNetwork(it) }
            .filter { it.episodes.isNotEmpty() }
            .filter { if (!showSpecials()) !it.isSpecial() else true }

        val episodes = seasons.flatMap { it.episodes }

        transactions.withTransaction {
          val localSeasons = localSource.seasons.getAllByShowId(tmdbId.id)
          val localEpisodes = localSource.episodes.getAllByShowId(tmdbId.id)
          val lastWatchedAt = localEpisodes.maxByOrNull { it.lastWatchedAt != null }?.lastWatchedAt?.toMillis() ?: 0L

          showsRepository.myShows.insert(tmdbId, lastWatchedAt)

          val seasonsToAdd = mutableListOf<SeasonDb>()
          val episodesToAdd = mutableListOf<EpisodeDb>()

          seasons.forEach { season ->
            if (localSeasons.none { it.idTmdb == season.ids.tmdb.id }) {
              seasonsToAdd.add(mappers.season.toDatabase(season, tmdbId, false))
            }
          }
          episodes.forEach { episode ->
            if (localEpisodes.none { it.idTmdb == episode.ids.tmdb.id }) {
              val season = seasons.find { it.number == episode.season }!!
              episodesToAdd.add(mappers.episode.toDatabase(episode, season, tmdbId, false, null, null))
            }
          }

          localSource.seasons.upsert(seasonsToAdd)
          localSource.episodes.upsert(episodesToAdd)
        }

        pinnedItemsRepository.removePinnedItem(show)
        announcementManager.refreshShowsAnnouncements()
      }

    suspend fun removeFromMyShows(
      tmdbId: IdTmdb,
      removeLocalData: Boolean,
    ) = withContext(dispatchers.IO) {
      val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdbId))
      transactions.withTransaction {
        showsRepository.myShows.delete(show.ids.tmdb)

        if (removeLocalData) {
          localSource.episodes.deleteAllUnwatchedForShow(show.tmdbId)
          val seasons = localSource.seasons.getAllByShowId(show.tmdbId)
          val episodes = localSource.episodes.getAllByShowId(show.tmdbId)
          val toDelete = mutableListOf<SeasonDb>()
          seasons.forEach { season ->
            if (episodes.none { it.idSeason == season.idTmdb }) {
              toDelete.add(season)
            }
          }
          localSource.seasons.delete(toDelete)
        }

        pinnedItemsRepository.removePinnedItem(show)
        announcementManager.refreshShowsAnnouncements()
      }
    }

    private suspend fun showSpecials() =
      withContext(dispatchers.IO) {
        settingsRepository.load().specialSeasonsEnabled
      }
  }
