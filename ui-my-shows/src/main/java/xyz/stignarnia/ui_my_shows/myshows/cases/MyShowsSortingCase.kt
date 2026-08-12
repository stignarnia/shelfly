package xyz.stignarnia.ui_my_shows.myshows.cases

import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_model.MyShowsSection
import xyz.stignarnia.ui_model.MyShowsSection.ALL
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class MyShowsSortingCase @Inject constructor(
  private val settingsRepository: SettingsRepository,
) {

  fun loadSectionSortOrder(section: MyShowsSection) =
    when (section) {
      ALL -> Pair(
        settingsRepository.sorting.myShowsAllSortOrder,
        settingsRepository.sorting.myShowsAllSortType,
      )
      else -> error("Should not be used here.")
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
