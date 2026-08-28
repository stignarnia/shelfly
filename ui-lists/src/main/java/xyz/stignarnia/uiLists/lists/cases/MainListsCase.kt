package xyz.stignarnia.uiLists.lists.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Mode.MOVIES
import xyz.stignarnia.common.Mode.SHOWS
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.CustomListItem
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiLists.lists.helpers.ListsItemImage
import xyz.stignarnia.uiLists.lists.helpers.ListsSorter
import xyz.stignarnia.uiLists.lists.recycler.ListsItem
import xyz.stignarnia.uiModel.CustomList
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType.POSTER
import javax.inject.Inject

@ViewModelScoped
class MainListsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
    private val listsRepository: ListsRepository,
    private val dateProvider: DateFormatProvider,
    private val settingsRepository: SettingsRepository,
    private val showImagesProvider: ShowImagesProvider,
    private val movieImagesProvider: MovieImagesProvider,
    private val sorter: ListsSorter,
  ) {
    companion object {
      private const val IMAGES_LIMIT = 3
    }

    suspend fun loadLists(searchQuery: String?) =
      withContext(dispatchers.IO) {
        val lists = listsRepository.loadAll()
        val dateFormat = dateProvider.loadFullDayFormat()
        val sorting =
          Pair(
            settingsRepository.sorting.listsAllSortOrder,
            settingsRepository.sorting.listsAllSortType,
          )

        lists
          .filterByQuery(searchQuery)
          .sortedWith(sorter.sort(sorting.first, sorting.second))
          .map {
            async {
              val items = localSource.customListsItems.getItemsForListImages(it.id, IMAGES_LIMIT)
              val images = mutableListOf<ListsItemImage>()
              val unavailable = ListsItemImage(Image.createUnavailable(POSTER))
              items.forEach { item ->
                images.add(findImage(item) ?: unavailable)
              }
              if (images.size < IMAGES_LIMIT) {
                (images.size..IMAGES_LIMIT).forEach { _ -> images.add(unavailable) }
              }
              ListsItem(it, images, sorting, dateFormat)
            }
          }.awaitAll()
      }

    private fun List<CustomList>.filterByQuery(query: String?) =
      when {
        query.isNullOrBlank() -> {
          this
        }

        else -> {
          this.filter {
            it.name.contains(query, ignoreCase = true) ||
              it.description?.contains(query, ignoreCase = true) == true
          }
        }
      }

    private suspend fun findImage(item: CustomListItem) =
      when (item.type) {
        SHOWS.type -> {
          val showDb = localSource.shows.getById(item.idTmdb)
          showDb?.let {
            val show = mappers.show.fromDatabase(it)
            val image = showImagesProvider.findCachedImage(show, POSTER)
            ListsItemImage(image, show = show)
          }
        }

        MOVIES.type -> {
          val movieDb = localSource.movies.getById(item.idTmdb)
          movieDb?.let {
            val movie = mappers.movie.fromDatabase(movieDb)
            val image = movieImagesProvider.findCachedImage(movie, POSTER)
            ListsItemImage(image, movie = movie)
          }
        }

        else -> {
          throw IllegalStateException()
        }
      }
  }
