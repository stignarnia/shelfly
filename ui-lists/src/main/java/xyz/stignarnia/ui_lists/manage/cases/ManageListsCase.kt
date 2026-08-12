package xyz.stignarnia.ui_lists.manage.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.ui_lists.manage.recycler.ManageListsItem
import xyz.stignarnia.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ManageListsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val listsRepository: ListsRepository,
) {

  suspend fun loadLists(
    itemId: IdTmdb,
    itemType: String,
  ) = withContext(dispatchers.IO) {
    val listsAsync = async { listsRepository.loadAll() }
    val listsWithItemAsync = async { listsRepository.loadListIdsForItem(itemId, itemType) }
    val (lists, listsWithItem) = Pair(listsAsync.await(), listsWithItemAsync.await())
    lists
      .sortedBy { it.name }
      .map {
        val isChecked = listsWithItem.contains(it.id)
        ManageListsItem(it, isChecked, true)
      }
  }

  suspend fun addToList(
    itemId: IdTmdb,
    itemType: String,
    listItem: ManageListsItem,
  ) = withContext(dispatchers.IO) {
    listsRepository.addToList(listItem.list.id, itemId, itemType)
  }

  suspend fun removeFromList(
    itemId: IdTmdb,
    itemType: String,
    listItem: ManageListsItem,
  ) = withContext(dispatchers.IO) {
    listsRepository.removeFromList(listItem.list.id, itemId, itemType)
  }
}
