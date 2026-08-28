package xyz.stignarnia.uiLists.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.CustomList
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import javax.inject.Inject

@ViewModelScoped
class ListDetailsSortCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
  ) {
    suspend fun setSortOrder(
      listId: Long,
      sortOrder: SortOrder,
      sortType: SortType,
    ): CustomList =
      withContext(dispatchers.IO) {
        localSource.customLists.updateSortByLocal(
          listId,
          sortOrder.slug,
          sortType.slug,
          nowUtcMillis(),
        )
        val list = localSource.customLists.getById(listId)!!
        mappers.customList.fromDatabase(list)
      }

    suspend fun setFilterTypes(
      listId: Long,
      types: List<Mode>,
    ): CustomList =
      withContext(dispatchers.IO) {
        localSource.customLists.updateFilterTypeLocal(
          listId,
          types.joinToString(",") { it.type },
          nowUtcMillis(),
        )
        val list = localSource.customLists.getById(listId)!!
        mappers.customList.fromDatabase(list)
      }
  }
