package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.CustomListItem

interface CustomListsItemsLocalDataSource {
  suspend fun update(items: List<CustomListItem>)

  suspend fun getListsForItem(
    idTmdb: Long,
    type: String,
  ): List<Long>

  suspend fun getByIdTmdb(
    idList: Long,
    idTmdb: Long,
    type: String,
  ): CustomListItem?

  suspend fun getItemsById(idList: Long): List<CustomListItem>

  suspend fun getItemsForListImages(
    idList: Long,
    limit: Int,
  ): List<CustomListItem>

  suspend fun getRankForList(idList: Long): Long?

  suspend fun insertItem(item: CustomListItem)

  suspend fun deleteItem(
    idList: Long,
    idTmdb: Long,
    type: String,
  )
}
