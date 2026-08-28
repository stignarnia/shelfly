package xyz.stignarnia.uiMyShows.myshows.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiModel.MyShowsSection
import xyz.stignarnia.uiModel.MyShowsSection.ALL
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import javax.inject.Inject

@ViewModelScoped
class MyShowsSortingCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun loadSectionSortOrder(section: MyShowsSection) =
      when (section) {
        ALL -> {
          Pair(
            settingsRepository.sorting.myShowsAllSortOrder,
            settingsRepository.sorting.myShowsAllSortType,
          )
        }

        else -> {
          error("Should not be used here.")
        }
      }

    fun setSectionSortOrder(
      section: MyShowsSection,
      sortOrder: SortOrder,
      sortType: SortType,
    ) = when (section) {
      ALL -> {
        settingsRepository.sorting.myShowsAllSortOrder = sortOrder
        settingsRepository.sorting.myShowsAllSortType = sortType
      }

      else -> {
        error("Should not be used here.")
      }
    }
  }
