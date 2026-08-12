package xyz.stignarnia.ui_my_movies.mymovies.cases

import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class MyMoviesSortingCase @Inject constructor(
  private val settingsRepository: SettingsRepository,
) {

  fun loadSortOrder() =
    Pair(
      settingsRepository.sorting.myMoviesAllSortOrder,
      settingsRepository.sorting.myMoviesAllSortType,
    )

  fun setSortOrder(
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    settingsRepository.sorting.myMoviesAllSortOrder = sortOrder
    settingsRepository.sorting.myMoviesAllSortType = sortType
  }
}
