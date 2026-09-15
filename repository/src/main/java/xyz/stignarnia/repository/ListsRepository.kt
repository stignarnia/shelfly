package xyz.stignarnia.repository

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.CustomListItem
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.CustomList
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListsRepository
  @Inject
  constructor(
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
      val list =
        CustomList.create().copy(
          idTmdb = idTmdb,
          idSlug = idSlug ?: ListIdentity.create(),
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
      val updated =
        listDb.copy(
          name = name,
          idTmdb = idTmdb ?: listDb.idTmdb,
          idSlug = idSlug ?: listDb.idSlug,
          description = description,
          updatedAt = nowUtcMillis(),
        )
      localSource.customLists.update(listOf(updated))
      return mappers.customList.fromDatabase(updated)
    }

    /**
     * Gives every list that predates [ListIdentity] an identity of its own.
     * Backup, import and sync call this before reading list identities, rather than relying on a one-off database migration.
     *
     * The update timestamp is left alone: being assigned an identity is not an edit, and sync orders a list's edits against its deletions by that timestamp.
     */
    suspend fun ensureIdentities() {
      val missing = localSource.customLists.getAll().filterNot { ListIdentity.isValid(it.idSlug) }
      if (missing.isEmpty()) return
      localSource.customLists.update(missing.map { it.copy(idSlug = ListIdentity.create()) })
    }

    suspend fun deleteList(listId: Long) = localSource.customLists.deleteById(listId)

    /**
     * Moves every item of [sourceListId] into [targetListId], then deletes the source list.
     * Items the target already has are skipped, and the rest are appended after the target's own in the source's order, keeping when they were listed.
     *
     * Each moved item is stamped with the time of the merge, so a removal of the same item from the target that sync already carries cannot outrank this deliberate addition.
     */
    suspend fun mergeLists(
      sourceListId: Long,
      targetListId: Long,
    ) {
      val now = nowUtcMillis()
      transactions.withTransaction {
        localSource.customListsItems
          .getItemsById(sourceListId)
          .forEach { item ->
            localSource.customListsItems.insertItem(item.copy(id = 0, idList = targetListId, updatedAt = now))
          }
        localSource.customLists.deleteById(sourceListId)
        localSource.customLists.updateTimestamp(targetListId, now)
      }
    }

    suspend fun addToList(
      listId: Long,
      itemTmdbId: IdTmdb,
      itemType: String,
      listedAt: Long = nowUtcMillis(),
      createdAt: Long = nowUtcMillis(),
      updatedAt: Long = nowUtcMillis(),
    ) {
      val itemDb =
        CustomListItem(
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
