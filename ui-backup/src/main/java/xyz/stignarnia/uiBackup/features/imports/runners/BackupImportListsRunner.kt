package xyz.stignarnia.uiBackup.features.imports.runners

import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.common.extensions.toUtcDateTime
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.repository.ListIdentity
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Importing
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedItem
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedList
import xyz.stignarnia.uiBackup.model.BackupList
import xyz.stignarnia.uiBackup.model.BackupListItem
import xyz.stignarnia.uiBackup.model.BackupLists
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiModel.CustomList
import xyz.stignarnia.uiModel.IdTmdb
import java.io.IOException
import javax.inject.Inject
import xyz.stignarnia.dataLocal.database.model.CustomList as CustomListDb

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
    val failedLists = mutableListOf<BackupUnmatchedList>()

    override suspend fun run(backup: BackupLists, startCount: Int, total: Int): Int {
      failedLists.clear()
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
        listsRepository.ensureIdentities()
        val localLists = localSource.customLists.getAll()
        val totalItems = backup.lists.sumOf { it.items.size }
        var currentItem = startCount

        for (backupList in backup.lists) {
          if (totalItems == 0) {
            currentItem++
            updateProgress(backupList.name, currentItem, total)
          }

          // A backup list is an existing local list only when both carry the same identity.
          // A backup from before lists had one can only be matched by name, and that match is reported, so joining an unrelated list of the same name is never silent.
          val hasIdentity = ListIdentity.isValid(backupList.slugId)
          val localList =
            if (hasIdentity) {
              localLists.find { it.idSlug == backupList.slugId }
            } else {
              localLists.find { it.name == backupList.name }
            }

          currentItem =
            if (localList != null) {
              importExistingCustomList(backupList, localList, isMatchedByName = !hasIdentity, currentItem, total)
            } else {
              val idSlug = if (hasIdentity) backupList.slugId else ListIdentity.create()
              importNewCustomList(backupList, idSlug, currentItem, total)
            }
        }
        currentItem
      }

    private suspend fun importNewCustomList(
      backupList: BackupList,
      idSlug: String,
      startItemCount: Int,
      total: Int,
    ): Int {
      var currentItem = startItemCount
      val list =
        CustomList.create().copy(
          idTmdb = null,
          idSlug = idSlug,
          name = backupList.name,
          description = backupList.description,
        )
      val listDb = mappers.customList.toDatabase(list)
      // An insert the conflict strategy ignored comes back as -1, not as a missing row id.
      val listId =
        localSource.customLists
          .insert(listOf(listDb))
          .firstOrNull()
          ?.takeIf { it > 0 }
      if (listId == null) {
        Timber.w("Failed to create list ${backupList.name}")
        failedLists += BackupUnmatchedList(title = backupList.name, reason = "Could not create the list in the local database.")
        return currentItem
      }

      // Add items to the list
      val failedItems = mutableListOf<BackupUnmatchedItem>()
      for (item in backupList.items) {
        currentItem++
        updateProgress(backupList.name, currentItem, total)
        importItem(listId, item)?.let { failedItems += it }
      }
      reportList(backupList, failedItems)
      return currentItem
    }

    private suspend fun importExistingCustomList(
      backupList: BackupList,
      localList: CustomListDb,
      isMatchedByName: Boolean,
      startItemCount: Int,
      total: Int,
    ): Int {
      var currentItem = startItemCount
      val localListItems = listsRepository.loadListItemsForId(localList.id)

      val failedItems = mutableListOf<BackupUnmatchedItem>()
      for (item in backupList.items) {
        currentItem++
        updateProgress(backupList.name, currentItem, total)

        val itemExists = localListItems.any { it.idTmdb == item.tmdbId && it.type == item.type }
        if (itemExists) {
          continue
        }

        importItem(localList.id, item)?.let { failedItems += it }
      }
      val reason = if (isMatchedByName) "Merged into the existing list with the same name." else null
      reportList(backupList, failedItems, reason)
      return currentItem
    }

    /**
     * Adds one item to a list, returning why it could not be added, or null once it has been.
     * A failure is recorded rather than thrown, so an item whose details will not fetch does not abort every list after it.
     */
    private suspend fun importItem(
      listId: Long,
      item: BackupListItem,
    ): BackupUnmatchedItem? =
      try {
        importDetails(item)
        listsRepository.addToList(
          listId = listId,
          itemTmdbId = IdTmdb(item.tmdbId),
          itemType = item.type,
          listedAt = item.listedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
          createdAt = item.createdAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
          updatedAt = item.updatedAt.toUtcDateTime()?.toMillis() ?: nowUtcMillis(),
        )
        null
      } catch (error: Throwable) {
        rethrowCancellation(error)
        val reason =
          when {
            error is HttpException && error.code() == 404 -> "Details not found on TMDB (HTTP 404)."
            error is HttpException -> "TMDB API error (${error.code()} ${error.message()})."
            error is IOException -> "Network error fetching TMDB details (${error.message ?: "timeout"})."
            else -> "Failed to add to list: ${error.message ?: error.javaClass.simpleName}."
          }
        Timber.w("Failed to add ${item.type} ${item.tmdbId} to list $listId - $reason")
        BackupUnmatchedItem(title = itemTitle(item), reason = reason, tmdbId = item.tmdbId)
      }

    /**
     * A backup list item carries no title, so the one cached locally is used when there is one.
     */
    private suspend fun itemTitle(item: BackupListItem): String {
      val localTitle =
        when (item.type) {
          "show" -> localSource.shows.getById(item.tmdbId)?.title
          "movie" -> localSource.movies.getById(item.tmdbId)?.title
          else -> null
        }
      if (!localTitle.isNullOrBlank()) {
        return localTitle
      }
      return when (item.type) {
        "show" -> "Show (TMDB ID: ${item.tmdbId})"
        "movie" -> "Movie (TMDB ID: ${item.tmdbId})"
        else -> "TMDB ID: ${item.tmdbId}"
      }
    }

    private fun reportList(
      backupList: BackupList,
      failedItems: List<BackupUnmatchedItem>,
      reason: String? = null,
    ) {
      if (reason != null || failedItems.isNotEmpty()) {
        failedLists += BackupUnmatchedList(title = backupList.name, reason = reason, unmatchedItems = failedItems)
      }
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
