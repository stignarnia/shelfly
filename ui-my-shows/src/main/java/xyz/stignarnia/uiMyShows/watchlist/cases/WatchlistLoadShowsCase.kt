package xyz.stignarnia.uiMyShows.watchlist.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem
import xyz.stignarnia.uiMyShows.watchlist.helpers.WatchlistItemFilter
import xyz.stignarnia.uiMyShows.watchlist.helpers.WatchlistItemSorter
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@ViewModelScoped
class WatchlistLoadShowsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsCase: WatchlistRatingsCase,
    private val sorter: WatchlistItemSorter,
    private val filters: WatchlistItemFilter,
    private val showsRepository: ShowsRepository,
    private val translationsRepository: TranslationsRepository,
    private val settingsRepository: SettingsRepository,
    private val imagesProvider: ShowImagesProvider,
    private val dateFormatProvider: DateFormatProvider,
  ) {
    suspend fun loadShows(searchQuery: String): List<CollectionListItem> =
      withContext(dispatchers.IO) {
        val language = translationsRepository.getLanguage()
        val ratings = ratingsCase.loadRatings()
        val dateFormat = dateFormatProvider.loadFullDayFormat()
        val translations =
          if (language == Config.DEFAULT_LANGUAGE) {
            emptyMap()
          } else {
            translationsRepository.loadAllShowsLocal(language)
          }
        val spoilers = settingsRepository.spoilers.getAll()

        var filtersItem = loadFiltersItem()
        val filtersNetworks = filtersItem.networks
        val filtersGenres = filtersItem.genres.map { it.slug.lowercase() }

        val showsItems =
          showsRepository.watchlistShows
            .loadAll()
            .map {
              toListItemAsync(
                show = it,
                translation = translations[it.tmdbId],
                userRating = ratings[it.ids.tmdb],
                dateFormat = dateFormat,
                sortOrder = filtersItem.sortOrder,
                spoilers = spoilers,
              )
            }.awaitAll()
            .filter { item ->
              filters.filterByQuery(item, searchQuery) &&
                filters.filterUpcoming(item, filtersItem.upcoming) &&
                filters.filterNetworks(item, filtersNetworks) &&
                filters.filterGenres(item, filtersGenres)
            }.sortedWith(sorter.sort(filtersItem.sortOrder, filtersItem.sortType))

        filtersItem = filtersItem.copy(count = showsItems.size)

        if (showsItems.isNotEmpty() || filtersItem.hasActiveFilters()) {
          listOf(filtersItem) + showsItems
        } else {
          showsItems
        }
      }

    private fun loadFiltersItem(): CollectionListItem.FiltersItem =
      CollectionListItem.FiltersItem(
        sortOrder = settingsRepository.sorting.watchlistShowsSortOrder,
        sortType = settingsRepository.sorting.watchlistShowsSortType,
        networks = settingsRepository.filters.watchlistShowsNetworks,
        genres = settingsRepository.filters.watchlistShowsGenres,
        upcoming = settingsRepository.filters.watchlistShowsUpcoming,
        count = 0,
      )

    private fun CoroutineScope.toListItemAsync(
      show: Show,
      translation: Translation?,
      userRating: UserRating?,
      dateFormat: DateTimeFormatter,
      sortOrder: SortOrder,
      spoilers: SpoilersSettings,
    ) = async {
      val image = imagesProvider.findCachedImage(show, ImageType.POSTER)
      CollectionListItem.ShowItem(
        isLoading = false,
        show = show,
        image = image,
        dateFormat = dateFormat,
        translation = translation,
        userRating = userRating?.rating,
        sortOrder = sortOrder,
        spoilers =
          CollectionListItem.ShowItem.Spoilers(
            isSpoilerHidden = spoilers.isWatchlistShowsHidden,
            isSpoilerRatingsHidden = spoilers.isWatchlistShowsRatingsHidden,
            isSpoilerTapToReveal = spoilers.isTapToReveal,
          ),
      )
    }
  }
