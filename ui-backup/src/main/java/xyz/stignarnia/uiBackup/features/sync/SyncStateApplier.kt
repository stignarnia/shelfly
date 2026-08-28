package xyz.stignarnia.uiBackup.features.sync

import timber.log.Timber
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.Rating
import xyz.stignarnia.dataLocal.database.model.Season
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.uiBackup.features.sync.model.SyncEntity
import xyz.stignarnia.uiBackup.model.BackupScheme
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

/**
 * Applies the half of a merge the importer cannot: taking things away.
 *
 * [SyncMerge] returns the state every device should end up with, and the existing import worker can add whatever is missing - but it is additive by design, so on its own it would leave behind everything the merge decided is gone.
 * Rather than teach it to delete, the removals are worked out here the same way tombstones are: by diffing.
 * Anything present in local state and absent from the merged state has to go.
 *
 * That is deliberately the same trick [SyncTombstoneDeriver] uses, and for the same reason - a diff cannot forget a case, whereas a hand-written list of removal rules can.
 *
 * Removals run before the import so the two never fight over one entity: a show moved from the watchlist into the collection is a removal from one and an addition to the other, and doing the addition first would only have it removed again.
 */
internal class SyncStateApplier
  @Inject
  constructor(
    private val localSource: LocalDataSource,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val onHoldItemsRepository: OnHoldItemsRepository,
    private val transactions: TransactionsProvider,
  ) {
    suspend fun apply(
      local: BackupScheme,
      merged: BackupScheme,
    ) {
      val removals = SyncStateFlattener.flatten(local).keys - SyncStateFlattener.flatten(merged).keys
      if (removals.isEmpty()) {
        Timber.d("Nothing to remove.")
        return
      }

      val byEntity = removals.groupBy({ it.first }, { it.second })
      Timber.d("Removing ${removals.size} entities: ${byEntity.mapValues { it.value.size }}")

      transactions.withTransaction {
        // Watch state first, while the collection still says what it said when the user made the change.
        // Unwatching an episode of a show that is no longer followed deletes the row instead of clearing it, so doing this after the collection removals would throw away progress that the app itself would have kept.
        removeWatchedEpisodes(byEntity[SyncEntity.EPISODE_WATCHED].orEmpty())
        removeWatchedSeasons(byEntity[SyncEntity.SEASON_WATCHED].orEmpty())

        // Items before their lists, so a list that is going away entirely does not have its items deleted out from under it twice.
        removeListItems(byEntity[SyncEntity.CUSTOM_LIST_ITEM].orEmpty())
        byEntity[SyncEntity.CUSTOM_LIST].orEmpty().forEach { key ->
          key.toLongOrNull()?.let { localSource.customLists.deleteById(it) }
        }

        removeCollections(byEntity)
        removeRatings(byEntity)
      }

      // Pinned and on-hold live in SharedPreferences, so they cannot join the transaction above.
      // They are pure UI state - the worst a partial failure costs is a pin, and the next sync restates it.
      byEntity[SyncEntity.PINNED_SHOW].orEmpty().forEach { key ->
        key.toLongOrNull()?.let { pinnedItemsRepository.removeShowPinnedItem(IdTmdb(it)) }
      }
      byEntity[SyncEntity.PINNED_MOVIE].orEmpty().forEach { key ->
        key.toLongOrNull()?.let { pinnedItemsRepository.removeMoviePinnedItem(IdTmdb(it)) }
      }
      byEntity[SyncEntity.ON_HOLD_SHOW].orEmpty().forEach { key ->
        key.toLongOrNull()?.let { onHoldItemsRepository.removeItem(IdTmdb(it)) }
      }
    }

    private suspend fun removeCollections(byEntity: Map<SyncEntity, List<String>>) {
      byEntity[SyncEntity.MY_SHOW].orEmpty().forEachId { localSource.myShows.deleteById(it) }
      byEntity[SyncEntity.WATCHLIST_SHOW].orEmpty().forEachId { localSource.watchlistShows.deleteById(it) }
      byEntity[SyncEntity.HIDDEN_SHOW].orEmpty().forEachId { localSource.archiveShows.deleteById(it) }
      byEntity[SyncEntity.MY_MOVIE].orEmpty().forEachId { localSource.myMovies.deleteById(it) }
      byEntity[SyncEntity.WATCHLIST_MOVIE].orEmpty().forEachId { localSource.watchlistMovies.deleteById(it) }
      byEntity[SyncEntity.HIDDEN_MOVIE].orEmpty().forEachId { localSource.archiveMovies.deleteById(it) }
    }

    private suspend fun removeRatings(byEntity: Map<SyncEntity, List<String>>) {
      byEntity[SyncEntity.SHOW_RATING].orEmpty().forEachId {
        localSource.ratings.deleteByKey(it, Rating.TYPE_SHOW, Rating.NO_NUMBER, Rating.NO_NUMBER)
      }
      byEntity[SyncEntity.MOVIE_RATING].orEmpty().forEachId {
        localSource.ratings.deleteByKey(it, Rating.TYPE_MOVIE, Rating.NO_NUMBER, Rating.NO_NUMBER)
      }
      byEntity[SyncEntity.SEASON_RATING].orEmpty().forEach { key ->
        val (showId, seasonNumber) = key.showAndSeason() ?: return@forEach
        localSource.ratings.deleteByKey(showId, Rating.TYPE_SEASON, seasonNumber, Rating.NO_NUMBER)
      }
      byEntity[SyncEntity.EPISODE_RATING].orEmpty().forEach { key ->
        val (showId, seasonNumber, episodeNumber) = key.showSeasonAndEpisode() ?: return@forEach
        localSource.ratings.deleteByKey(showId, Rating.TYPE_EPISODE, seasonNumber, episodeNumber)
      }
    }

    /**
     * Mirrors `EpisodesManager.setEpisodeUnwatched` without going through it.
     *
     * That method wants a full `EpisodeBundle` - episode, season and show as domain objects - which would mean three loads per episode to undo one flag.
     * The rows are already here, so the same two rules are applied directly: a followed show keeps the episode and loses its watched marks, an unfollowed one loses the row, because for those the table is only a cache.
     */
    private suspend fun removeWatchedEpisodes(keys: List<String>) {
      keys
        .mapNotNull { it.showSeasonAndEpisode() }
        .groupBy { it.first }
        .forEach { (showTmdbId, entries) ->
          val localEpisodes = localSource.episodes.getAllByShowId(showTmdbId)
          val targets =
            entries.mapNotNull { (_, seasonNumber, episodeNumber) ->
              localEpisodes.find { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
            }
          if (targets.isEmpty()) {
            return@forEach
          }

          if (localSource.myShows.checkExists(showTmdbId)) {
            localSource.episodes.upsert(
              targets.map { it.copy(isWatched = false, lastExportedAt = null, lastWatchedAt = null) },
            )
          } else {
            localSource.episodes.delete(targets)
          }

          refreshSeasonsWatched(showTmdbId, targets.map { it.idSeason }.distinct())
        }
    }

    /**
     * A season is watched when all of its episodes are, so unwatching an episode can unwatch its season too - the same bookkeeping `onEpisodeSet` does.
     */
    private suspend fun refreshSeasonsWatched(
      showTmdbId: Long,
      seasonIds: List<Long>,
    ) {
      val seasons = localSource.seasons.getAllByShowId(showTmdbId)
      val updated =
        seasonIds.mapNotNull { seasonId ->
          val season = seasons.find { it.idTmdb == seasonId } ?: return@mapNotNull null
          val watchedCount =
            localSource.episodes
              .getAllForSeason(seasonId)
              .count { it.isWatched }
          val isWatched = watchedCount == season.episodesCount
          season.takeIf { it.isWatched != isWatched }?.copy(isWatched = isWatched)
        }
      if (updated.isNotEmpty()) {
        localSource.seasons.update(updated)
      }
    }

    private suspend fun removeWatchedSeasons(keys: List<String>) {
      keys
        .mapNotNull { it.showAndSeason() }
        .groupBy { it.first }
        .forEach { (showTmdbId, entries) ->
          val seasons = localSource.seasons.getAllByShowId(showTmdbId)
          val targets: List<Season> =
            entries.mapNotNull { (_, seasonNumber) ->
              seasons.find { it.seasonNumber == seasonNumber && it.isWatched }
            }
          if (targets.isNotEmpty()) {
            localSource.seasons.update(targets.map { it.copy(isWatched = false) })
          }
        }
    }

    private suspend fun removeListItems(keys: List<String>) {
      keys.forEach { key ->
        val parts = key.split(SEPARATOR)
        if (parts.size != 3) {
          Timber.w("Unreadable list item key: $key")
          return@forEach
        }
        val listId = parts[0].toLongOrNull() ?: return@forEach
        val tmdbId = parts[2].toLongOrNull() ?: return@forEach
        localSource.customListsItems.deleteItem(idList = listId, idTmdb = tmdbId, type = parts[1])
      }
    }

    // Keys are built by SyncEntity.key, so a key that will not parse came from a peer writing something this version does not understand.
    // Skipping it loses one removal; guessing at it could delete the wrong row.

    private fun String.showAndSeason(): Pair<Long, Int>? {
      val parts = split(SEPARATOR)
      if (parts.size != 2) return null.also { Timber.w("Unreadable key: $this") }
      val showId = parts[0].toLongOrNull() ?: return null
      val seasonNumber = parts[1].toIntOrNull() ?: return null
      return showId to seasonNumber
    }

    private fun String.showSeasonAndEpisode(): Triple<Long, Int, Int>? {
      val parts = split(SEPARATOR)
      if (parts.size != 3) return null.also { Timber.w("Unreadable key: $this") }
      val showId = parts[0].toLongOrNull() ?: return null
      val seasonNumber = parts[1].toIntOrNull() ?: return null
      val episodeNumber = parts[2].toIntOrNull() ?: return null
      return Triple(showId, seasonNumber, episodeNumber)
    }

    private suspend fun List<String>.forEachId(action: suspend (Long) -> Unit) {
      forEach { key ->
        val id = key.toLongOrNull()
        if (id == null) {
          Timber.w("Unreadable key: $key")
          return@forEach
        }
        action(id)
      }
    }

    private companion object {
      const val SEPARATOR = ":"
    }
  }
