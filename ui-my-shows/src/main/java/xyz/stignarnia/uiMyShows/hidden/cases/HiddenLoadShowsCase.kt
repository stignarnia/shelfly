package xyz.stignarnia.uiMyShows.hidden.cases

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
import xyz.stignarnia.uiBase.utilities.extensions.removeDiacritics
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiModel.UpcomingFilter
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem
import xyz.stignarnia.uiMyShows.hidden.helpers.HiddenItemSorter
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@ViewModelScoped
class HiddenLoadShowsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsCase: HiddenRatingsCase,
    private val sorter: HiddenItemSorter,
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

        val sortOrder = settingsRepository.sorting.hiddenShowsSortOrder
        val sortType = settingsRepository.sorting.hiddenShowsSortType

        var filtersItem = loadFiltersItem(sortOrder, sortType)
        val filtersNetworks = filtersItem.networks
        val filtersGenres = filtersItem.genres.map { it.slug.lowercase() }

        val hiddenItems =
          showsRepository.hiddenShows
            .loadAll()
            .map {
              toListItemAsync(
                show = it,
                translation = translations[it.tmdbId],
                userRating = ratings[it.ids.tmdb],
                dateFormat = dateFormat,
                sortOrder = sortOrder,
                spoilers = spoilers,
              )
            }.awaitAll()
            .filterByQuery(searchQuery)
            .filterByNetwork(filtersNetworks)
            .filterByGenre(filtersGenres)
            .sortedWith(sorter.sort(sortOrder, sortType))

        filtersItem = filtersItem.copy(count = hiddenItems.size)

        if (hiddenItems.isNotEmpty() || filtersItem.hasActiveFilters()) {
          listOf(filtersItem) + hiddenItems
        } else {
          hiddenItems
        }
      }

    private fun List<CollectionListItem.ShowItem>.filterByQuery(query: String) =
      filter {
        it.show.title
          .removeDiacritics()
          .contains(query, true) ||
          it.translation
            ?.title
            ?.removeDiacritics()
            ?.contains(query, true) == true
      }

    private fun List<CollectionListItem.ShowItem>.filterByNetwork(networks: List<String>) =
      filter { networks.isEmpty() || it.show.network in networks }

    private fun List<CollectionListItem.ShowItem>.filterByGenre(genres: List<String>) =
      filter { genres.isEmpty() || it.show.genres.any { genre -> genre.lowercase() in genres } }

    private fun loadFiltersItem(
      sortOrder: SortOrder,
      sortType: SortType,
    ): CollectionListItem.FiltersItem =
      CollectionListItem.FiltersItem(
        sortOrder = sortOrder,
        sortType = sortType,
        networks = settingsRepository.filters.hiddenShowsNetworks,
        genres = settingsRepository.filters.hiddenShowsGenres,
        upcoming = UpcomingFilter.OFF,
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
        translation = translation,
        userRating = userRating?.rating,
        dateFormat = dateFormat,
        sortOrder = sortOrder,
        spoilers =
          CollectionListItem.ShowItem.Spoilers(
            isSpoilerHidden = spoilers.isHiddenShowsHidden,
            isSpoilerRatingsHidden = spoilers.isHiddenShowsRatingsHidden,
            isSpoilerTapToReveal = spoilers.isTapToReveal,
          ),
      )
    }
  }
