package xyz.stignarnia.ui_discover.cases

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.isSameDayOrAfter
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toUtcDateTime
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_discover.helpers.itemtype.ImageTypeProvider
import xyz.stignarnia.ui_discover.recycler.DiscoverListItem
import xyz.stignarnia.ui_model.DiscoverFeed
import xyz.stignarnia.ui_model.DiscoverFilters
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
internal class DiscoverShowsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val showsRepository: ShowsRepository,
  private val imageTypeProvider: ImageTypeProvider,
  private val imagesProvider: ShowImagesProvider,
  private val translationsRepository: TranslationsRepository,
  private val settingsRepository: SettingsRepository,
) {

  suspend fun isCacheValid() =
    withContext(dispatchers.IO) {
      showsRepository.discoverShows.isCacheValid()
    }

  suspend fun loadCachedShows(filters: DiscoverFilters) =
    withContext(dispatchers.IO) {
      val myShowsIds = async { showsRepository.myShows.loadAllIds() }
      val watchlistShowsIds = async { showsRepository.watchlistShows.loadAllIds() }
      val archiveShowsIds = async { showsRepository.hiddenShows.loadAllIds() }
      val cachedShows = async { showsRepository.discoverShows.loadAllCached() }

      prepareItems(
        shows = cachedShows.await(),
        myShowsIds = myShowsIds.await(),
        watchlistShowsIds = watchlistShowsIds.await(),
        hiddenShowsIds = archiveShowsIds.await(),
        filters = filters,
      )
    }

  suspend fun loadRemoteShows(filters: DiscoverFilters) =
    withContext(dispatchers.IO) {
      val showCollection = !filters.hideCollection
      val genres = filters.genres.toList()
      val networks = filters.networks.toList()

      val myAsync = async { showsRepository.myShows.loadAllIds() }
      val watchlistSync = async { showsRepository.watchlistShows.loadAllIds() }
      val archiveAsync = async { showsRepository.hiddenShows.loadAllIds() }
      val (myIds, watchlistIds, hiddenIds) = awaitAll(myAsync, watchlistSync, archiveAsync)
      val collectionSize = myIds.size + watchlistIds.size + hiddenIds.size

      val remoteShows = showsRepository.discoverShows.loadAllRemote(
        order = filters.feedOrder,
        showCollection = showCollection,
        collectionSize = collectionSize,
        genres = genres,
        networks = networks,
      )

      showsRepository.discoverShows.cacheDiscoverShows(remoteShows)

      prepareItems(
        shows = remoteShows,
        myShowsIds = myIds,
        watchlistShowsIds = watchlistIds,
        hiddenShowsIds = hiddenIds,
        filters = filters,
      )
    }

  private suspend fun prepareItems(
    shows: List<Show>,
    myShowsIds: List<Long>,
    watchlistShowsIds: List<Long>,
    hiddenShowsIds: List<Long>,
    filters: DiscoverFilters,
  ) = coroutineScope {
    val language = translationsRepository.getLanguage()
    val collectionIds = myShowsIds + watchlistShowsIds + hiddenShowsIds
    shows
      .filter { it.tmdbId !in hiddenShowsIds }
      .filter {
        if (!filters.hideCollection) {
          true
        } else {
          it.tmdbId !in collectionIds
        }
      }.sortedBy(filters.feedOrder)
      .mapIndexed { index, show ->
        async {
          val itemType = imageTypeProvider.getImageType(index)
          val image = imagesProvider.findCachedImage(show, itemType)
          val translation = loadTranslation(language, itemType, show)
          DiscoverListItem(
            show = show,
            image = image,
            isFollowed = show.tmdbId in myShowsIds,
            isWatchlist = show.tmdbId in watchlistShowsIds,
            translation = translation,
          )
        }
      }.awaitAll()
      .toMutableList()
      .toList()
  }

  private suspend fun loadTranslation(
    language: String,
    itemType: ImageType,
    show: Show,
  ) = if (language == Config.DEFAULT_LANGUAGE || itemType == ImageType.POSTER) {
    null
  } else {
    translationsRepository.loadTranslation(show, language, true)
  }

  private fun List<Show>.sortedBy(order: DiscoverFeed): List<Show> {
    val nowUtc = nowUtc()
    return when (order) {
      DiscoverFeed.RECENT -> {
        this
          .filter {
            it.firstAired.isNotBlank() &&
              nowUtc.isSameDayOrAfter(it.firstAired.toUtcDateTime() ?: return@filter false)
          }.sortedWith(compareByDescending { it.firstAired })
      }
      else -> {
        this
      }
    }
  }
}
