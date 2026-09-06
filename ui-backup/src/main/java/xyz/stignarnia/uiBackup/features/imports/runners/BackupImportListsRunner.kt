package xyz.stignarnia.uiBackup.features.imports.runners

import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcDateTime
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Importing
import xyz.stignarnia.uiBackup.model.BackupList
import xyz.stignarnia.uiBackup.model.BackupListItem
import xyz.stignarnia.uiBackup.model.BackupLists
import xyz.stignarnia.uiModel.CustomList
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

internal class BackupImportListsRunner
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val listsRepository: ListsRepository,
    private val showsRepository: ShowsRepository,
    private val moviesRepository: MoviesRepository,
    private val mappers: Mappers,
  ) : BackupImportRunner<BackupLists>() {
    override suspend fun run(backup: BackupLists, startCount: Int, total: Int): Int {
      Timber.d("Initialized.")
      return runImport(backup, startCount, total)
        .also {
          Timber.d("Success.")
        }
    }

    private suspend fun runImport(
      backup: BackupLists,
      startCount: Int,
      total: Int,
    ): Int =
      withContext(dispatchers.IO) {
        val localLists = localSource.customLists.getAll()
        val totalItems = backup.lists.sumOf { it.items.size }
        var currentItem = startCount

        for (backupList in backup.lists) {
          if (totalItems == 0) {
            currentItem++
            updateProgress(backupList.name, currentItem, total)
          }

          if (localLists.any { it.id == backupList.id }) {
            // Custom lists already exists locally
            currentItem = importExistingCustomList(backupList, currentItem, total)
          } else {
            // Custom list does not exist locally
            currentItem = importNewCustomList(backupList, currentItem, total)
          }
        }
        currentItem
      }

    private suspend fun importNewCustomList(
      backupList: BackupList,
      startItemCount: Int,
      total: Int,
    ): Int {
      var currentItem = startItemCount
      val list =
        CustomList.create().copy(
          idTmdb = null,
          idSlug = backupList.slugId,
          name = backupList.name,
          description = backupList.description,
        )
      val listDb = mappers.customList.toDatabase(list)
      val listId = localSource.customLists.insert(listOf(listDb)).firstOrNull() ?: return currentItem

      // Add items to the list
      for (item in backupList.items) {
        currentItem++
        updateProgress(backupList.name, currentItem, total)
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
      return currentItem
    }

    private suspend fun importExistingCustomList(
      backupList: BackupList,
      startItemCount: Int,
      total: Int,
    ): Int {
      var currentItem = startItemCount
      val localList = localSource.customLists.getById(backupList.id) ?: return currentItem
      val localListItems = listsRepository.loadListItemsForId(localList.id)

      for (item in backupList.items) {
        currentItem++
        updateProgress(backupList.name, currentItem, total)

        val itemExists = localListItems.any { it.idTmdb == item.tmdbId && it.type == item.type }
        if (itemExists) {
          continue
        }

        importDetails(item)

        listsRepository.addToList(
          listId = localList.id,
          itemTmdbId = IdTmdb(item.tmdbId),
          itemType = item.type,
          listedAt = item.listedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
          createdAt = item.createdAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
          updatedAt = item.updatedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
        )
      }
      return currentItem
    }

    private suspend fun updateProgress(title: String, current: Int, total: Int) {
      statusListener?.invoke(Importing(title, current = current, total = total))
    }

    private suspend fun importDetails(backupItem: BackupListItem) {
      if (backupItem.type == "show") {
        showsRepository.detailsShow.load(IdTmdb(backupItem.tmdbId))
      } else if (backupItem.type == "movie") {
        moviesRepository.movieDetails.load(IdTmdb(backupItem.tmdbId))
      }
    }
  }
