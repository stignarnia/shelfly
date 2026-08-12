package com.michaldrabik.ui_show.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.common.extensions.toMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.PinnedItemsRepository
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.repository.shows.ShowsRepository
import com.michaldrabik.ui_base.notifications.AnnouncementManager
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.michaldrabik.data_local.database.model.Episode as EpisodeDb
import com.michaldrabik.data_local.database.model.Season as SeasonDb

@ViewModelScoped
class ShowDetailsMyShowsCase @Inject constructor(
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
      val (myShows, watchlistShows) = awaitAll(
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
