package xyz.stignarnia.ui_backup.features.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import xyz.stignarnia.ui_backup.features.sync.model.SyncEntity
import xyz.stignarnia.ui_backup.features.sync.model.SyncPayload
import xyz.stignarnia.ui_backup.features.sync.model.SyncTombstoneEntry
import xyz.stignarnia.ui_backup.model.BackupList
import xyz.stignarnia.ui_backup.model.BackupListItem
import xyz.stignarnia.ui_backup.model.BackupLists
import xyz.stignarnia.ui_backup.model.BackupMovies
import xyz.stignarnia.ui_backup.model.BackupScheme
import xyz.stignarnia.ui_backup.model.BackupShow
import xyz.stignarnia.ui_backup.model.BackupShows

class SyncMergeTest {

  @Test
  fun `Should union what each device has`() {
    val local = scheme(myShows = listOf(show(1, JAN)))
    val peer = payload("tablet", scheme(myShows = listOf(show(2, JAN))))

    val merged = SyncMerge.merge(local, emptyList(), listOf(peer))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(1L, 2L)
  }

  @Test
  fun `Should not delete something a peer simply has not heard of`() {
    // The tablet has been offline for a month: its state is small and stale.
    // Absence must never mean deletion, or it would wipe the phone's library.
    val local = scheme(myShows = listOf(show(1, JAN), show(2, JAN), show(3, JAN)))
    val staleTablet = payload("tablet", scheme(myShows = listOf(show(1, JAN))))

    val merged = SyncMerge.merge(local, emptyList(), listOf(staleTablet))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(1L, 2L, 3L)
  }

  @Test
  fun `Should propagate a deletion a peer vouches for`() {
    val local = scheme(myShows = listOf(show(1, JAN), show(2, JAN)))
    val tablet = payload(
      "tablet",
      scheme(myShows = listOf(show(1, JAN))),
      tombstones = listOf(tombstone(SyncEntity.MY_SHOW, "2", FEB)),
    )

    val merged = SyncMerge.merge(local, emptyList(), listOf(tablet))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(1L)
  }

  @Test
  fun `Should let a deliberate re-add overrule an older deletion`() {
    // Deleted on the tablet in January, added back on the phone in February.
    val local = scheme(myShows = listOf(show(2, FEB)))
    val tablet = payload("tablet", scheme(), tombstones = listOf(tombstone(SyncEntity.MY_SHOW, "2", JAN)))

    val merged = SyncMerge.merge(local, emptyList(), listOf(tablet))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(2L)
  }

  @Test
  fun `Should keep the entity when a deletion ties with its sighting`() {
    val local = scheme(myShows = listOf(show(2, JAN)))
    val tablet = payload("tablet", scheme(), tombstones = listOf(tombstone(SyncEntity.MY_SHOW, "2", JAN)))

    val merged = SyncMerge.merge(local, emptyList(), listOf(tablet))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(2L)
  }

  @Test
  fun `Should survive the stale peer race by re-publishing adopted tombstones`() {
    // The phone deleted show 2 and published the tombstone while the tablet was offline.
    // The laptop saw it and now carries it.
    // When the tablet finally syncs, it still hears about the deletion from the laptop rather than resurrecting the show.
    val tabletLocal = scheme(myShows = listOf(show(1, JAN), show(2, JAN)))
    val laptop = payload(
      "laptop",
      scheme(myShows = listOf(show(1, JAN))),
      tombstones = listOf(tombstone(SyncEntity.MY_SHOW, "2", FEB)),
    )

    val merged = SyncMerge.merge(tabletLocal, emptyList(), listOf(laptop))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(1L)
    // The tablet now vouches for the deletion too, so it keeps travelling.
    assertThat(merged.tombstones.map { it.entity to it.key })
      .contains(SyncEntity.MY_SHOW to "2")
  }

  @Test
  fun `Should keep the newest tombstone when devices disagree on when`() {
    val local = scheme(myShows = listOf(show(2, MAR)))
    val peerA = payload("a", scheme(), tombstones = listOf(tombstone(SyncEntity.MY_SHOW, "2", JAN)))
    val peerB = payload("b", scheme(), tombstones = listOf(tombstone(SyncEntity.MY_SHOW, "2", FEB)))

    val merged = SyncMerge.merge(local, emptyList(), listOf(peerA, peerB))

    assertThat(merged.tombstones.single { it.key == "2" }.deletedAt).isEqualTo(FEB)
    // Re-added in March, after both deletions, so it stays.
    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(2L)
  }

  @Test
  fun `Should merge three devices at once`() {
    val local = scheme(myShows = listOf(show(1, JAN)))
    val peerA = payload("a", scheme(myShows = listOf(show(2, JAN))))
    val peerB = payload("b", scheme(myShows = listOf(show(3, JAN))))

    val merged = SyncMerge.merge(local, emptyList(), listOf(peerA, peerB))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(1L, 2L, 3L)
  }

  @Test
  fun `Should treat a move between collections as a deletion in the one it left`() {
    // The phone moved show 5 from the watchlist into the collection.
    val local = scheme(myShows = listOf(show(5, FEB)))
    val tablet = payload("tablet", scheme(watchlist = listOf(show(5, JAN))))
    val tombstones = listOf(tombstone(SyncEntity.WATCHLIST_SHOW, "5", FEB))

    val merged = SyncMerge.merge(local, tombstones, listOf(tablet))

    assertThat(
      merged.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactly(5L)
    assertThat(merged.state.shows.collectionWatchlist).isEmpty()
  }

  @Test
  fun `Should gather list items from every device's copy of the same list`() {
    // Each device added a different item to the same list.
    // Keeping only the winning copy of the list would silently drop the other's item.
    val local = scheme(lists = listOf(list(1, JAN, listOf(item(1, "show", 10)))))
    val tablet = payload("tablet", scheme(lists = listOf(list(1, FEB, listOf(item(1, "movie", 20))))))

    val merged = SyncMerge.merge(local, emptyList(), listOf(tablet))

    val items = merged.state.lists.lists
      .single()
      .items
    assertThat(items.map { it.type to it.tmdbId }).containsExactly("show" to 10L, "movie" to 20L)
    assertThat(
      merged.state.lists.lists
        .single()
        .itemCount,
    ).isEqualTo(2)
  }

  @Test
  fun `Should remove a single list item without touching the rest of the list`() {
    val local = scheme(lists = listOf(list(1, JAN, listOf(item(1, "show", 10), item(1, "movie", 20)))))
    val tombstones = listOf(tombstone(SyncEntity.CUSTOM_LIST_ITEM, "1:movie:20", FEB))

    val merged = SyncMerge.merge(local, tombstones, emptyList())

    val items = merged.state.lists.lists
      .single()
      .items
    assertThat(items.map { it.tmdbId }).containsExactly(10L)
  }

  @Test
  fun `Should be order independent across peers`() {
    val local = scheme(myShows = listOf(show(1, JAN)))
    val peerA = payload("a", scheme(myShows = listOf(show(2, JAN))))
    val peerB = payload("b", scheme(), tombstones = listOf(tombstone(SyncEntity.MY_SHOW, "2", FEB)))

    val forwards = SyncMerge.merge(local, emptyList(), listOf(peerA, peerB))
    val backwards = SyncMerge.merge(local, emptyList(), listOf(peerB, peerA))

    assertThat(
      forwards.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactlyElementsIn(
      backwards.state.shows.collectionHistory
        .map { it.tmdbId },
    )
  }

  @Test
  fun `Should be idempotent when nothing has changed`() {
    val local = scheme(myShows = listOf(show(1, JAN)))
    val peer = payload("tablet", scheme(myShows = listOf(show(2, JAN))))

    val once = SyncMerge.merge(local, emptyList(), listOf(peer))
    val twice = SyncMerge.merge(once.state, once.tombstones, listOf(peer))

    assertThat(
      twice.state.shows.collectionHistory
        .map { it.tmdbId },
    ).containsExactlyElementsIn(
      once.state.shows.collectionHistory
        .map { it.tmdbId },
    )
  }

  private companion object {
    const val JAN = 1_700_000_000_000L
    const val FEB = 1_702_000_000_000L
    const val MAR = 1_704_000_000_000L

    fun iso(millis: Long): String =
      java.time.Instant
        .ofEpochMilli(millis)
        .toString()

    fun scheme(
      myShows: List<BackupShow> = emptyList(),
      watchlist: List<BackupShow> = emptyList(),
      lists: List<BackupList> = emptyList(),
    ) = BackupScheme(
      version = 3,
      platform = "android",
      createdAt = "2026-01-01T00:00:00Z",
      shows = BackupShows(collectionHistory = myShows, collectionWatchlist = watchlist),
      movies = BackupMovies(),
      lists = BackupLists(lists = lists),
    )

    fun show(
      id: Long,
      addedAt: Long,
    ) = BackupShow(tmdbId = id, title = "Show $id", addedAt = iso(addedAt), updatedAt = iso(addedAt))

    fun list(
      id: Long,
      updatedAt: Long,
      items: List<BackupListItem>,
    ) = BackupList(
      id = id,
      slugId = "list-$id",
      name = "List $id",
      description = null,
      privacy = "private",
      itemCount = items.size.toLong(),
      createdAt = iso(JAN),
      updatedAt = iso(updatedAt),
      items = items,
    )

    fun item(
      listId: Long,
      type: String,
      tmdbId: Long,
    ) = BackupListItem(
      id = tmdbId,
      listId = listId,
      tmdbId = tmdbId,
      type = type,
      rank = 1,
      listedAt = iso(JAN),
      createdAt = iso(JAN),
      updatedAt = iso(JAN),
    )

    fun payload(
      deviceId: String,
      state: BackupScheme,
      tombstones: List<SyncTombstoneEntry> = emptyList(),
    ) = SyncPayload(deviceId = deviceId, updatedAt = JAN, state = state, tombstones = tombstones)

    fun tombstone(
      entity: SyncEntity,
      key: String,
      deletedAt: Long,
    ) = SyncTombstoneEntry(entity = entity, key = key, deletedAt = deletedAt)
  }
}
