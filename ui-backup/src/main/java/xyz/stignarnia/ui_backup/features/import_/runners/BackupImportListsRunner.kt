package xyz.stignarnia.ui_backup.features.import_.runners

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcDateTime
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_backup.features.import_.model.BackupImportStatus.Importing
import xyz.stignarnia.ui_backup.model.BackupList
import xyz.stignarnia.ui_backup.model.BackupListItem
import xyz.stignarnia.ui_backup.model.BackupLists
import xyz.stignarnia.ui_model.CustomList
import xyz.stignarnia.ui_model.IdTmdb
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal class BackupImportListsRunner @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val localSource: LocalDataSource,
  private val listsRepository: ListsRepository,
  private val showsRepository: ShowsRepository,
  private val moviesRepository: MoviesRepository,
  private val mappers: Mappers,
) : BackupImportRunner<BackupLists>() {

  override suspend fun run(backup: BackupLists) {
    Timber.d("Initialized.")
    runImport(backup)
      .also {
        Timber.d("Success.")
      }
  }

  private suspend fun runImport(backup: BackupLists) {
    withContext(dispatchers.IO) {
      val localLists = localSource.customLists.getAll()
      for (backupList in backup.lists) {
        statusListener?.invoke(Importing(backupList.name))

        if (localLists.any { it.id == backupList.id }) {
          // Custom lists already exists locally
          importExistingCustomList(backupList)
        } else {
          // Custom list does not exist locally
          importNewCustomList(backupList)
        }
      }
    }
  }

  private suspend fun importNewCustomList(backupList: BackupList) {
    val list = CustomList.create().copy(
      idTmdb = null,
      idSlug = backupList.slugId,
      name = backupList.name,
      description = backupList.description,
    )
    val listDb = mappers.customList.toDatabase(list)
    val listId = localSource.customLists.insert(listOf(listDb)).firstOrNull() ?: return

    // Add items to the list
    backupList.items.forEach { item ->
      importDetails(item)
      listsRepository.addToList(
        listId = listId,
        itemTmdbId = IdTmdb(item.tmdbId),
        itemType = item.type,
        listedAt = item.listedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
        createdAt = item.createdAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
        updatedAt = item.updatedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
      )
    }
  }

  private suspend fun importExistingCustomList(backupList: BackupList) {
    val localList = localSource.customLists.getById(backupList.id) ?: return
    val localListItems = listsRepository.loadListItemsForId(localList.id)

    for (backupItem in backupList.items) {
      val itemExists = localListItems.any { it.idTmdb == backupItem.tmdbId && it.type == backupItem.type }
      if (itemExists) {
        continue
      }

      importDetails(backupItem)

      listsRepository.addToList(
        listId = localList.id,
        itemTmdbId = IdTmdb(backupItem.tmdbId),
        itemType = backupItem.type,
        listedAt = backupItem.listedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
        createdAt = backupItem.createdAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
        updatedAt = backupItem.updatedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
      )
    }
  }

  private suspend fun importDetails(backupItem: BackupListItem) {
    if (backupItem.type == "show") {
      showsRepository.detailsShow.load(IdTmdb(backupItem.tmdbId))
    } else if (backupItem.type == "movie") {
      moviesRepository.movieDetails.load(IdTmdb(backupItem.tmdbId))
    }
  }
}
