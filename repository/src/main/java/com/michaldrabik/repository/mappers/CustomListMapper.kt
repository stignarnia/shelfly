package com.michaldrabik.repository.mappers

import com.michaldrabik.common.Mode
import com.michaldrabik.common.extensions.toMillis
import com.michaldrabik.ui_model.CustomList
import com.michaldrabik.ui_model.SortOrder
import com.michaldrabik.ui_model.SortType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import com.michaldrabik.data_local.database.model.CustomList as CustomListDb

class CustomListMapper @Inject constructor() {

  fun fromDatabase(list: CustomListDb) =
    CustomList(
      id = list.id,
      idTmdb = list.idTmdb,
      idSlug = list.idSlug,
      name = list.name,
      description = list.description,
      privacy = list.privacy,
      displayNumbers = list.displayNumbers,
      allowComments = list.allowComments,
      sortBy = SortOrder.fromSlug(list.sortBy) ?: SortOrder.RANK,
      sortHow = SortType.fromSlug(list.sortHow),
      sortByLocal = SortOrder.fromSlug(list.sortByLocal) ?: SortOrder.RANK,
      sortHowLocal = SortType.fromSlug(list.sortHowLocal),
      filterTypeLocal = when {
        list.filterTypeLocal.isEmpty() -> emptyList()
        else -> list.filterTypeLocal.split(",").map { Mode.fromType(it) }
      },
      itemCount = list.itemCount,
      commentCount = list.commentCount,
      likes = list.likes,
      createdAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(list.createdAt), ZoneId.of("UTC")),
      updatedAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(list.updatedAt), ZoneId.of("UTC")),
    )

  fun toDatabase(list: CustomList) =
    CustomListDb(
      id = list.id,
      idTmdb = list.idTmdb,
      idSlug = list.idSlug,
      name = list.name,
      description = list.description,
      privacy = list.privacy,
      displayNumbers = list.displayNumbers,
      allowComments = list.allowComments,
      sortBy = list.sortBy.slug,
      sortHow = list.sortHow.slug,
      sortByLocal = list.sortByLocal.slug,
      sortHowLocal = list.sortHowLocal.slug,
      filterTypeLocal = list.filterTypeLocal.joinToString(",") { it.type },
      itemCount = list.itemCount,
      commentCount = list.commentCount,
      likes = list.likes,
      createdAt = list.createdAt.toMillis(),
      updatedAt = list.updatedAt.toMillis(),
    )
}
