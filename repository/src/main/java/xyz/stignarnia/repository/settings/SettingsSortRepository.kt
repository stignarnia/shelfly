package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import xyz.stignarnia.repository.utilities.BooleanPreference
import xyz.stignarnia.repository.utilities.EnumPreference
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_ADDED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.RECENTLY_WATCHED
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SortType.ASCENDING
import xyz.stignarnia.uiModel.SortType.DESCENDING
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsSortRepository
  @Inject
  constructor(
    @param:Named("miscPreferences") private var preferences: SharedPreferences,
  ) {
    companion object Key {
      private const val PROGRESS_SHOWS_SORT_ORDER = "PROGRESS_SHOWS_SORT_ORDER"
      private const val PROGRESS_SHOWS_SORT_TYPE = "PROGRESS_SHOWS_SORT_TYPE"
      private const val PROGRESS_SHOWS_NEW_AT_TOP = "PROGRESS_SHOWS_NEW_AT_TOP"
      private const val WATCHLIST_SHOWS_SORT_ORDER = "WATCHLIST_SHOWS_SORT_ORDER"
      private const val WATCHLIST_SHOWS_SORT_TYPE = "WATCHLIST_SHOWS_SORT_TYPE"
      private const val HIDDEN_SHOWS_SORT_ORDER = "HIDDEN_SHOWS_SORT_ORDER"
      private const val HIDDEN_SHOWS_SORT_TYPE = "HIDDEN_SHOWS_SORT_TYPE"
      private const val MY_SHOWS_ALL_SORT_ORDER = "MY_SHOWS_ALL_SORT_ORDER"
      private const val MY_SHOWS_ALL_SORT_TYPE = "MY_SHOWS_ALL_SORT_TYPE"

      private const val PROGRESS_MOVIES_SORT_ORDER = "PROGRESS_MOVIES_SORT_ORDER"
      private const val PROGRESS_MOVIES_SORT_TYPE = "PROGRESS_MOVIES_SORT_TYPE"
      private const val WATCHLIST_MOVIES_SORT_ORDER = "WATCHLIST_MOVIES_SORT_ORDER"
      private const val WATCHLIST_MOVIES_SORT_TYPE = "WATCHLIST_MOVIES_SORT_TYPE"
      private const val HIDDEN_MOVIES_SORT_ORDER = "HIDDEN_MOVIES_SORT_ORDER"
      private const val HIDDEN_MOVIES_SORT_TYPE = "HIDDEN_MOVIES_SORT_TYPE"
      private const val MY_MOVIES_ALL_SORT_ORDER = "MY_MOVIES_ALL_SORT_ORDER"
      private const val MY_MOVIES_ALL_SORT_TYPE = "MY_MOVIES_ALL_SORT_TYPE"

      private const val LISTS_SORT_ORDER = "LISTS_SORT_ORDER"
      private const val LISTS_SORT_TYPE = "LISTS_SORT_TYPE"
    }

    var progressShowsNewAtTop by BooleanPreference(preferences, PROGRESS_SHOWS_NEW_AT_TOP, false)
    var progressShowsSortOrder by EnumPreference(
      preferences,
      PROGRESS_SHOWS_SORT_ORDER,
      RECENTLY_WATCHED,
      SortOrder::class.java,
    )
    var progressShowsSortType by EnumPreference(preferences, PROGRESS_SHOWS_SORT_TYPE, DESCENDING, SortType::class.java)
    var watchlistShowsSortOrder by EnumPreference(
      preferences,
      WATCHLIST_SHOWS_SORT_ORDER,
      DATE_ADDED,
      SortOrder::class.java,
    )
    var watchlistShowsSortType by EnumPreference(preferences, WATCHLIST_SHOWS_SORT_TYPE, DESCENDING, SortType::class.java)
    var hiddenShowsSortOrder by EnumPreference(preferences, HIDDEN_SHOWS_SORT_ORDER, DATE_ADDED, SortOrder::class.java)
    var hiddenShowsSortType by EnumPreference(preferences, HIDDEN_SHOWS_SORT_TYPE, DESCENDING, SortType::class.java)
    var myShowsAllSortOrder by EnumPreference(preferences, MY_SHOWS_ALL_SORT_ORDER, DATE_ADDED, SortOrder::class.java)
    var myShowsAllSortType by EnumPreference(preferences, MY_SHOWS_ALL_SORT_TYPE, DESCENDING, SortType::class.java)

    var progressMoviesSortOrder by EnumPreference(
      preferences,
      PROGRESS_MOVIES_SORT_ORDER,
      DATE_ADDED,
      SortOrder::class.java,
    )
    var progressMoviesSortType by EnumPreference(preferences, PROGRESS_MOVIES_SORT_TYPE, DESCENDING, SortType::class.java)
    var watchlistMoviesSortOrder by EnumPreference(
      preferences,
      WATCHLIST_MOVIES_SORT_ORDER,
      DATE_ADDED,
      SortOrder::class.java,
    )
    var watchlistMoviesSortType by EnumPreference(
      preferences,
      WATCHLIST_MOVIES_SORT_TYPE,
      DESCENDING,
      SortType::class.java,
    )
    var hiddenMoviesSortOrder by EnumPreference(preferences, HIDDEN_MOVIES_SORT_ORDER, DATE_ADDED, SortOrder::class.java)
    var hiddenMoviesSortType by EnumPreference(preferences, HIDDEN_MOVIES_SORT_TYPE, DESCENDING, SortType::class.java)
    var myMoviesAllSortOrder by EnumPreference(preferences, MY_MOVIES_ALL_SORT_ORDER, DATE_ADDED, SortOrder::class.java)
    var myMoviesAllSortType by EnumPreference(preferences, MY_MOVIES_ALL_SORT_TYPE, DESCENDING, SortType::class.java)

    var listsAllSortOrder by EnumPreference(preferences, LISTS_SORT_ORDER, NAME, SortOrder::class.java)
    var listsAllSortType by EnumPreference(preferences, LISTS_SORT_TYPE, ASCENDING, SortType::class.java)
  }
