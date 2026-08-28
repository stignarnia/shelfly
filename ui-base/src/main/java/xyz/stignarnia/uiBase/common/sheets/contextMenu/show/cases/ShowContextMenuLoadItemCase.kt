package xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.OnHoldItemsRepository
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.helpers.ShowContextItem
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.ImageType
import javax.inject.Inject

@ViewModelScoped
class ShowContextMenuLoadItemCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val showsRepository: ShowsRepository,
    private val pinnedItemsRepository: PinnedItemsRepository,
    private val onHoldItemsRepository: OnHoldItemsRepository,
    private val imagesProvider: ShowImagesProvider,
    private val translationsRepository: TranslationsRepository,
    private val ratingsRepository: RatingsRepository,
    private val settingsRepository: SettingsRepository,
  ) {
    suspend fun loadItem(tmdbId: IdTmdb) =
      withContext(dispatchers.IO) {
        val show = showsRepository.detailsShow.load(tmdbId)
        val language = translationsRepository.getLanguage()
        val spoilers = settingsRepository.spoilers.getAll()

        val imageAsync = async { imagesProvider.loadRemoteImage(show, ImageType.POSTER) }
        val translationAsync =
          async { translationsRepository.loadTranslation(show, language = language, onlyLocal = true) }
        val ratingAsync = async { ratingsRepository.shows.loadRatings(listOf(show)) }

        val isMyShowAsync = async { showsRepository.myShows.exists(tmdbId) }
        val isWatchlistAsync = async { showsRepository.watchlistShows.exists(tmdbId) }
        val isHiddenAsync = async { showsRepository.hiddenShows.exists(tmdbId) }

        val isPinnedAsync = async { pinnedItemsRepository.isItemPinned(show) }
        val isOnHoldAsync = async { onHoldItemsRepository.isOnHold(show) }

        ShowContextItem(
          show = show,
          image = imageAsync.await(),
          translation = translationAsync.await(),
          userRating = ratingAsync.await().firstOrNull()?.rating,
          isMyShow = isMyShowAsync.await(),
          isWatchlist = isWatchlistAsync.await(),
          isHidden = isHiddenAsync.await(),
          isPinnedTop = isPinnedAsync.await(),
          isOnHold = isOnHoldAsync.await(),
          spoilers = spoilers,
        )
      }
  }
