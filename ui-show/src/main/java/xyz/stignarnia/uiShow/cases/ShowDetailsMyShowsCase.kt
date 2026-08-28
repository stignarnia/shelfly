package xyz.stignarnia.uiShow.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.notifications.AnnouncementManager
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject
import xyz.stignarnia.dataLocal.database.model.Episode as EpisodeDb
import xyz.stignarnia.dataLocal.database.model.Season as SeasonDb

@ViewModelScoped
class ShowDetailsMyShowsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
    private val transactions: TransactionsProvider,
    private val showsRepository: ShowsRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val announcementManager: AnnouncementManager,
  ) {
    suspend fun getAllIds() =
      withContext(dispatchers.IO) {
        val (myShows, watchlistShows) =
          awaitAll(
            async { showsRepository.myShows.loadAllIds() },
            async { showsRepository.watchlistShows.loadAllIds() },
          )
        Pair(myShows, watchlistShows)
      }

    suspend fun isMyShows(show: Show) =
      withContext(dispatchers.IO) {
        showsRepository.myShows.exists(show.ids.tmdb)
      }

    suspend fun addToMyShows(
      show: Show,
      seasons: List<Season>,
      episodes: List<Episode>,
    ) = withContext(dispatchers.IO) {
      transactions.withTransaction {
        val localSeasons = localSource.seasons.getAllByShowId(show.tmdbId)
        val localEpisodes = localSource.episodes.getAllByShowId(show.tmdbId)
        val lastWatchedAt = localEpisodes.maxByOrNull { it.lastWatchedAt != null }?.lastWatchedAt?.toMillis() ?: 0L

        showsRepository.myShows.insert(show.ids.tmdb, lastWatchedAt)

        val seasonsToAdd = mutableListOf<SeasonDb>()
        val episodesToAdd = mutableListOf<EpisodeDb>()

        seasons.forEach { season ->
          if (localSeasons.none { it.idTmdb == season.ids.tmdb.id }) {
            seasonsToAdd.add(mappers.season.toDatabase(season, show.ids.tmdb, false))
          }
        }
        episodes.forEach { episode ->
          if (localEpisodes.none { it.idTmdb == episode.ids.tmdb.id }) {
            val season = seasons.find { it.number == episode.season }!!
            episodesToAdd.add(mappers.episode.toDatabase(episode, season, show.ids.tmdb, false, null, null))
          }
        }

        localSource.seasons.upsert(seasonsToAdd)
        localSource.episodes.upsert(episodesToAdd)
      }

      pinnedItemsRepository.removePinnedItem(show)
      announcementManager.refreshShowsAnnouncements()
    }

    suspend fun removeFromMyShows(
      show: Show,
      removeLocalData: Boolean,
    ) = withContext(dispatchers.IO) {
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
  }
