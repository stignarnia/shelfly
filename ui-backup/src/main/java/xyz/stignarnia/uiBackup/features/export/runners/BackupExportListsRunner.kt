package xyz.stignarnia.uiBackup.features.export.runners

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.dateIsoStringFromMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.uiBackup.model.BackupList
import xyz.stignarnia.uiBackup.model.BackupListItem
import xyz.stignarnia.uiBackup.model.BackupLists
import javax.inject.Inject

internal class BackupExportListsRunner
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
  ) : BackupExportRunner<BackupLists>() {
    override suspend fun run(): BackupLists {
      Timber.d("Initialized.")
      return runExport()
        .also {
          Timber.d("Success.")
        }
    }

    private suspend fun runExport(): BackupLists =
      withContext(dispatchers.IO) {
        val exportLists = mutableListOf<BackupList>()

        val localLists = localSource.customLists.getAll()

        localLists.forEach { list ->
          val localItems = localSource.customListsItems.getItemsById(list.id)

          val localShowIds = localItems.filter { it.type == "show" }.map { it.idTmdb }
          val localMovieIds = localItems.filter { it.type == "movie" }.map { it.idTmdb }

          val localShowTmdbIdsAsync = async { localSource.shows.getAllTmdbIds(tmdbIds = localShowIds) }
          val localMovieTmdbIdsAsync = async { localSource.movies.getAllTmdbIds(tmdbIds = localMovieIds) }
          val (localShowTmdbIds, localMovieTmdbIds) = awaitAll(localShowTmdbIdsAsync, localMovieTmdbIdsAsync)

          val backupItems =
            localItems.map {
              BackupListItem(
                id = it.id,
                listId = list.id,
                tmdbId =
                  when (it.type) {
                    "show" -> localShowTmdbIds.getOrDefault(it.idTmdb, -1)
                    "movie" -> localMovieTmdbIds.getOrDefault(it.idTmdb, -1)
                    else -> -1
                  },
                type = it.type,
                rank = it.rank,
                listedAt = dateIsoStringFromMillis(it.listedAt),
                createdAt = dateIsoStringFromMillis(it.createdAt),
                updatedAt = dateIsoStringFromMillis(it.updatedAt),
              )
            }

          val backupList =
            BackupList(
              id = list.id,
              slugId = list.idSlug,
              name = list.name,
              description = list.description,
              privacy = list.privacy,
              itemCount = list.itemCount,
              createdAt = dateIsoStringFromMillis(list.createdAt),
              updatedAt = dateIsoStringFromMillis(list.updatedAt),
              items = backupItems,
            )

          exportLists.add(backupList)
        }

        BackupLists(
          lists = exportLists,
        )
      }
  }
