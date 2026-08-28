package xyz.stignarnia.uiLists.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.CustomListItem
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiLists.details.recycler.ListDetailsItem
import xyz.stignarnia.uiModel.CustomList
import javax.inject.Inject

@ViewModelScoped
class ListDetailsMainCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val transactions: TransactionsProvider,
    private val listsRepository: ListsRepository,
    private val settingsRepository: SettingsRepository,
  ) {
    suspend fun loadDetails(id: Long) =
      withContext(dispatchers.IO) {
        listsRepository.loadById(id)
      }

    suspend fun updateRanks(
      listId: Long,
      items: List<ListDetailsItem>,
    ): List<ListDetailsItem> =
      withContext(dispatchers.IO) {
        val now = nowUtcMillis()
        val listItems = listsRepository.loadItemsById(listId)
        val updateItems = mutableListOf<ListDetailsItem>()
        val updateItemsDb = mutableListOf<CustomListItem>()
        items.forEachIndexed { index, item ->
          val dbItem = listItems.first { it.id == item.id }.copy(rank = index + 1L, updatedAt = now)
          val updatedItem = item.copy(rank = index + 1L)
          updateItems.add(updatedItem)
          updateItemsDb.add(dbItem)
        }
        transactions.withTransaction {
          localSource.customListsItems.update(updateItemsDb)
          localSource.customLists.updateTimestamp(listId, now)
        }
        updateItems
      }

    suspend fun deleteList(listId: Long) =
      withContext(dispatchers.IO) {
        val list = listsRepository.loadById(listId)
        val listIdTmdb = list.idTmdb

        listsRepository.deleteList(listId)
      }

    suspend fun isQuickRemoveEnabled(list: CustomList) =
      withContext(dispatchers.IO) {
        false
      }
  }
