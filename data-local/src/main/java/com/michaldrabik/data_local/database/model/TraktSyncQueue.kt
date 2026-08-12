package com.michaldrabik.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trakt_sync_queue")
data class TraktSyncQueue(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long,
  @ColumnInfo(name = "id_tmdb") val idTmdb: Long,
  @ColumnInfo(name = "id_list") val idList: Long?,
  @ColumnInfo(name = "type") val type: String,
  @ColumnInfo(name = "operation") val operation: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {

  companion object {
    fun createEpisode(
      episodeTraktId: Long,
      showTraktId: Long?,
      createdAt: Long,
      updatedAt: Long,
      clearProgress: Boolean,
    ): TraktSyncQueue {
      val operation = if (clearProgress) Operation.ADD_WITH_CLEAR else Operation.ADD
      return TraktSyncQueue(0, episodeTraktId, showTraktId, Type.EPISODE.slug, operation.slug, createdAt, updatedAt)
    }

    fun createShowWatchlist(
      idTmdb: Long,
      createdAt: Long,
      updatedAt: Long,
    ) = TraktSyncQueue(0, idTmdb, null, Type.SHOW_WATCHLIST.slug, Operation.ADD.slug, createdAt, updatedAt)

    fun createMovie(
      idTmdb: Long,
      createdAt: Long,
      updatedAt: Long,
    ) = TraktSyncQueue(0, idTmdb, null, Type.MOVIE.slug, Operation.ADD.slug, createdAt, updatedAt)

    fun createMovieWatchlist(
      idTmdb: Long,
      createdAt: Long,
      updatedAt: Long,
    ) = TraktSyncQueue(0, idTmdb, null, Type.MOVIE_WATCHLIST.slug, Operation.ADD.slug, createdAt, updatedAt)

    fun createListShow(
      idTmdb: Long,
      idList: Long,
      operation: Operation,
      createdAt: Long,
      updatedAt: Long,
    ) = TraktSyncQueue(0, idTmdb, idList, Type.LIST_ITEM_SHOW.slug, operation.slug, createdAt, updatedAt)

    fun createListMovie(
      idTmdb: Long,
      idList: Long,
      operation: Operation,
      createdAt: Long,
      updatedAt: Long,
    ) = TraktSyncQueue(0, idTmdb, idList, Type.LIST_ITEM_MOVIE.slug, operation.slug, createdAt, updatedAt)

    fun createHiddenShow(
      idTmdb: Long,
      operation: Operation,
      createdAt: Long,
      updatedAt: Long,
    ) = TraktSyncQueue(0, idTmdb, null, Type.HIDDEN_SHOW.slug, operation.slug, createdAt, updatedAt)

    fun createHiddenMovie(
      idTmdb: Long,
      operation: Operation,
      createdAt: Long,
      updatedAt: Long,
    ) = TraktSyncQueue(0, idTmdb, null, Type.HIDDEN_MOVIE.slug, operation.slug, createdAt, updatedAt)
  }

  enum class Type(
    val slug: String,
  ) {
    EPISODE("episode"),
    SHOW_WATCHLIST("show_watchlist"),
    MOVIE("movie"),
    MOVIE_WATCHLIST("movie_watchlist"),
    LIST_ITEM_SHOW("list_item_show"),
    LIST_ITEM_MOVIE("list_item_movie"),
    HIDDEN_SHOW("hidden_show"),
    HIDDEN_MOVIE("hidden_movie"),
  }

  enum class Operation(
    val slug: String,
  ) {
    ADD("add"),
    ADD_WITH_CLEAR("add_clear"),
    REMOVE("remove"),
  }
}
