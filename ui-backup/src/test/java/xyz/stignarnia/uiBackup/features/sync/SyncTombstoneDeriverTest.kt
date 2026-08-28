package xyz.stignarnia.uiBackup.features.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import xyz.stignarnia.uiBackup.features.sync.model.SyncEntity
import xyz.stignarnia.uiBackup.model.BackupList
import xyz.stignarnia.uiBackup.model.BackupListItem
import xyz.stignarnia.uiBackup.model.BackupLists
import xyz.stignarnia.uiBackup.model.BackupMovie
import xyz.stignarnia.uiBackup.model.BackupMovies
import xyz.stignarnia.uiBackup.model.BackupScheme
import xyz.stignarnia.uiBackup.model.BackupShow
import xyz.stignarnia.uiBackup.model.BackupShows

class SyncTombstoneDeriverTest {
  @Test
  fun `Should derive nothing on a first sync, so a new device only ever adds`() {
    val local = scheme(myShows = listOf(show(1)))

    val tombstones = SyncTombstoneDeriver.derive(published = null, local = local, deletedAt = T1)

    assertThat(tombstones).isEmpty()
  }

  @Test
  fun `Should derive a deletion for something dropped since the last upload`() {
    val published = scheme(myShows = listOf(show(1), show(2)))
    val local = scheme(myShows = listOf(show(1)))

    val tombstones = SyncTombstoneDeriver.derive(published, local, T1)

    assertThat(tombstones.map { it.entity to it.key })
      .containsExactly(SyncEntity.MY_SHOW to "2")
    assertThat(tombstones.single().deletedAt).isEqualTo(T1)
  }

  @Test
  fun `Should derive nothing for something merely added`() {
    val published = scheme(myShows = listOf(show(1)))
    val local = scheme(myShows = listOf(show(1), show(2)))

    assertThat(SyncTombstoneDeriver.derive(published, local, T1)).isEmpty()
  }

  @Test
  fun `Should catch the hidden deletion when a show moves between collections`() {
    // Adding to the collection quietly removes the show from the watchlist.
    // Instrumenting call sites is what misses this; a diff cannot.
    val published = scheme(watchlist = listOf(show(7)))
    val local = scheme(myShows = listOf(show(7)))

    val tombstones = SyncTombstoneDeriver.derive(published, local, T1)

    assertThat(tombstones.map { it.entity to it.key })
      .containsExactly(SyncEntity.WATCHLIST_SHOW to "7")
  }

  @Test
  fun `Should catch cascading removals when a list is deleted`() {
    val published =
      scheme(
        lists = listOf(list(id = 1, items = listOf(listItem(1, "show", 10), listItem(1, "movie", 20)))),
      )
    val local = scheme()

    val tombstones = SyncTombstoneDeriver.derive(published, local, T1)

    assertThat(tombstones.map { it.entity to it.key }).containsExactly(
      SyncEntity.CUSTOM_LIST to "1",
      SyncEntity.CUSTOM_LIST_ITEM to "1:show:10",
      SyncEntity.CUSTOM_LIST_ITEM to "1:movie:20",
    )
  }

  @Test
  fun `Should treat every collection as its own entity`() {
    val published =
      scheme(
        myShows = listOf(show(1)),
        watchlist = listOf(show(1)),
        movies = listOf(movie(1)),
      )
    val local = scheme(myShows = listOf(show(1)))

    val tombstones = SyncTombstoneDeriver.derive(published, local, T1)

    // The same TMDB id in different collections is different state.
    assertThat(tombstones.map { it.entity to it.key }).containsExactly(
      SyncEntity.WATCHLIST_SHOW to "1",
      SyncEntity.MY_MOVIE to "1",
    )
  }

  private companion object {
    const val T1 = 1_700_000_000_000L

    fun scheme(
      myShows: List<BackupShow> = emptyList(),
      watchlist: List<BackupShow> = emptyList(),
      movies: List<BackupMovie> = emptyList(),
      lists: List<BackupList> = emptyList(),
    ) = BackupScheme(
      version = 3,
      platform = "android",
      createdAt = "2026-01-01T00:00:00Z",
      shows = BackupShows(collectionHistory = myShows, collectionWatchlist = watchlist),
      movies = BackupMovies(collectionHistory = movies),
      lists = BackupLists(lists = lists),
    )

    fun show(id: Long) =
      BackupShow(
        tmdbId = id,
        title = "Show $id",
        addedAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
      )

    fun movie(id: Long) = BackupMovie(tmdbId = id, title = "Movie $id", addedAt = "2026-01-01T00:00:00Z")

    fun list(
      id: Long,
      items: List<BackupListItem>,
    ) = BackupList(
      id = id,
      slugId = "list-$id",
      name = "List $id",
      description = null,
      privacy = "private",
      itemCount = items.size.toLong(),
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z",
      items = items,
    )

    fun listItem(
      listId: Long,
      type: String,
      tmdbId: Long,
    ) = BackupListItem(
      id = tmdbId,
      listId = listId,
      tmdbId = tmdbId,
      type = type,
      rank = 1,
      listedAt = "2026-01-01T00:00:00Z",
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z",
    )
  }
}
