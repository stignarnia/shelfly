package com.michaldrabik.ui_show.sections.seasons.helpers

import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_show.sections.seasons.recycler.SeasonListItem
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper memory cache of seasons for a current show details.
 */
@Singleton
class SeasonsCache @Inject constructor() {

  private val seasonsCache = Collections.synchronizedMap(mutableMapOf<IdTmdb, SeasonsBundle?>())

  fun setSeasons(
    showId: IdTmdb,
    seasons: List<SeasonListItem>,
    areSeasonsLocal: Boolean,
  ) {
    seasonsCache[showId] = SeasonsBundle(seasons.toList(), areSeasonsLocal)
  }

  fun loadSeasons(showId: IdTmdb): List<SeasonListItem>? = seasonsCache[showId]?.seasons

  fun hasSeasons(showId: IdTmdb): Boolean = seasonsCache[showId]?.seasons != null

  fun areSeasonsLocal(showId: IdTmdb): Boolean = seasonsCache[showId]?.isLocal ?: false

  fun clear(showId: IdTmdb) {
    seasonsCache.remove(showId)
  }
}
