package com.michaldrabik.repository

import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.CustomListItem
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.CustomList
import com.michaldrabik.ui_model.IdTmdb
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListsRepository @Inject constructor(
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
  private val transactions: TransactionsProvider,
) {

  suspend fun createList(
    name: String,
    description: String?,
    idTmdb: Long?,
    idSlug: String?,
  ): CustomList {
    val list = CustomList.create().copy(
      idTmdb = idTmdb,
      idSlug = idSlug ?: "",
      name = name.trim(),
      description = description?.trim(),
    )
    val listDb = mappers.customList.toDatabase(list)
    localSource.customLists.insert(listOf(listDb))
    return list
  }

  suspend fun updateList(
    id: Long,
    idTmdb: Long?,
    idSlug: String?,
    name: String,
    description: String?,
  ): CustomList {
    val listDb = localSource.customLists.getById(id)!!
    val updated = listDb.copy(
      name = name,
      idTmdb = idTmdb ?: listDb.idTmdb,
      idSlug = idSlug ?: listDb.idSlug,
      description = description,
      updatedAt = nowUtcMillis(),
    )
    localSource.customLists.update(listOf(updated))
    return mappers.customList.fromDatabase(updated)
  }

  suspend fun deleteList(listId: Long) = localSource.customLists.deleteById(listId)

  suspend fun addToList(
    listId: Long,
    itemTmdbId: IdTmdb,
    itemType: String,
    listedAt: Long = nowUtcMillis(),
    createdAt: Long = nowUtcMillis(),
    updatedAt: Long = nowUtcMillis(),
  ) {
    val itemDb = CustomListItem(
      rank = 0,
      idList = listId,
      idTmdb = itemTmdbId.id,
      type = itemType,
      listedAt = listedAt,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
    transactions.withTransaction {
      localSource.customListsItems.insertItem(itemDb)
      localSource.customLists.updateTimestamp(listId, nowUtcMillis())
    }
  }

  suspend fun removeFromList(
    listId: Long,
    itemTmdbId: IdTmdb,
    itemType: String,
  ) {
    transactions.withTransaction {
      localSource.customListsItems.deleteItem(listId, itemTmdbId.id, itemType)
      localSource.customLists.updateTimestamp(listId, nowUtcMillis())
    }
  }

  suspend fun loadListIdsForItem(
    itemTmdbId: IdTmdb,
    itemType: String,
  ) = localSource.customListsItems.getListsForItem(itemTmdbId.id, itemType)

  suspend fun loadListItemsForId(listId: Long) = localSource.customListsItems.getItemsById(listId)

  suspend fun loadById(listId: Long): CustomList {
    val listDb = localSource.customLists.getById(listId)!!
    return mappers.customList.fromDatabase(listDb)
  }

  suspend fun loadItemsById(listId: Long) = localSource.customListsItems.getItemsById(listId)

  suspend fun loadAll(): List<CustomList> {
    val listsDb = localSource.customLists.getAll()
    return listsDb.map { mappers.customList.fromDatabase(it) }
  }
}
