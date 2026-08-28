package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.stignarnia.repository.utilities.BooleanPreference
import xyz.stignarnia.repository.utilities.EnumPreference
import xyz.stignarnia.uiModel.DiscoverFeed
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.HistoryPeriod
import xyz.stignarnia.uiModel.MyShowsSection
import xyz.stignarnia.uiModel.StreamingProvider
import xyz.stignarnia.uiModel.UpcomingFilter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsFiltersRepository
  @Inject
  constructor(
    @param:Named("miscPreferences") private var preferences: SharedPreferences,
  ) {
    companion object Key {
      private const val PROGRESS_SHOWS_UPCOMING = "PROGRESS_SHOWS_UPCOMING"
      private const val PROGRESS_SHOWS_ON_HOLD = "PROGRESS_SHOWS_ON_HOLD"
      private const val CALENDAR_SHOWS_PREMIERES = "CALENDAR_SHOWS_PREMIERES"
      private const val HISTORY_SHOWS_PERIOD = "HISTORY_SHOWS_PERIOD"
      private const val MY_SHOWS_TYPE = "MY_SHOWS_TYPE"

      // Bumped when the collection network filters stopped storing the names of a fixed enum and started storing the broadcaster exactly as the show carries it.
      // Old values name no real network, so they are left behind.
      private const val MY_SHOWS_NETWORKS = "MY_SHOWS_NETWORKS_2"
      private const val MY_SHOWS_GENRES = "MY_SHOWS_GENRES"
      private const val WATCHLIST_SHOWS_UPCOMING = "WATCHLIST_SHOWS_UPCOMING_2"
      private const val WATCHLIST_SHOWS_NETWORKS = "WATCHLIST_SHOWS_NETWORKS_2"
      private const val WATCHLIST_SHOWS_GENRES = "WATCHLIST_SHOWS_GENRES"
      private const val HIDDEN_SHOWS_NETWORKS = "HIDDEN_SHOWS_NETWORKS_2"
      private const val HIDDEN_SHOWS_GENRES = "HIDDEN_SHOWS_GENRES"

      private const val MY_MOVIES_GENRES = "MY_MOVIES_GENRES"
      private const val WATCHLIST_MOVIES_UPCOMING = "WATCHLIST_MOVIES_UPCOMING_2"
      private const val WATCHLIST_MOVIES_GENRES = "WATCHLIST_MOVIES_GENRES"
      private const val HIDDEN_MOVIES_GENRES = "HIDDEN_MOVIES_GENRES"

      private const val DISCOVER_SHOWS_FEED = "DISCOVER_SHOWS_FEED"
      private const val DISCOVER_MOVIES_FEED = "DISCOVER_MOVIES_FEED"
      private const val DISCOVER_SHOWS_PROVIDERS = "DISCOVER_SHOWS_PROVIDERS"
      private const val DISCOVER_MOVIES_PROVIDERS = "DISCOVER_MOVIES_PROVIDERS"

      private const val PROVIDER_SEPARATOR = "|"
    }

    // Shows

    var progressShowsUpcoming by BooleanPreference(preferences, PROGRESS_SHOWS_UPCOMING, false)
    var progressShowsOnHold by BooleanPreference(preferences, PROGRESS_SHOWS_ON_HOLD, false)
    var calendarPremieresOnly by BooleanPreference(preferences, CALENDAR_SHOWS_PREMIERES, false)

    var historyShowsPeriod by EnumPreference(
      preferences,
      HISTORY_SHOWS_PERIOD,
      HistoryPeriod.LAST_30_DAYS,
      HistoryPeriod::class.java,
    )

    var myShowsType by EnumPreference(preferences, MY_SHOWS_TYPE, MyShowsSection.ALL, MyShowsSection::class.java)

    var myShowsNetworks: List<String>
      get() = preferences.getStringSet(MY_SHOWS_NETWORKS, emptySet()).orEmpty().sorted()
      set(value) {
        preferences.edit { putStringSet(MY_SHOWS_NETWORKS, value.toSet()) }
      }

    var myShowsGenres: List<Genre>
      get() {
        val filters = preferences.getStringSet(MY_SHOWS_GENRES, emptySet()) ?: emptySet()
        return filters.map { Genre.valueOf(it) }
      }
      set(value) {
        preferences.edit { putStringSet(MY_SHOWS_GENRES, value.map { it.name }.toSet()) }
      }

    var watchlistShowsUpcoming by EnumPreference(
      preferences,
      WATCHLIST_SHOWS_UPCOMING,
      UpcomingFilter.OFF,
      UpcomingFilter::class.java,
    )

    var watchlistShowsNetworks: List<String>
      get() = preferences.getStringSet(WATCHLIST_SHOWS_NETWORKS, emptySet()).orEmpty().sorted()
      set(value) {
        preferences.edit { putStringSet(WATCHLIST_SHOWS_NETWORKS, value.toSet()) }
      }

    var watchlistShowsGenres: List<Genre>
      get() {
        val filters = preferences.getStringSet(WATCHLIST_SHOWS_GENRES, emptySet()) ?: emptySet()
        return filters.map { Genre.valueOf(it) }
      }
      set(value) {
        preferences.edit { putStringSet(WATCHLIST_SHOWS_GENRES, value.map { it.name }.toSet()) }
      }

    var hiddenShowsNetworks: List<String>
      get() = preferences.getStringSet(HIDDEN_SHOWS_NETWORKS, emptySet()).orEmpty().sorted()
      set(value) {
        preferences.edit { putStringSet(HIDDEN_SHOWS_NETWORKS, value.toSet()) }
      }

    var hiddenShowsGenres: List<Genre>
      get() {
        val filters = preferences.getStringSet(HIDDEN_SHOWS_GENRES, emptySet()) ?: emptySet()
        return filters.map { Genre.valueOf(it) }
      }
      set(value) {
        preferences.edit { putStringSet(HIDDEN_SHOWS_GENRES, value.map { it.name }.toSet()) }
      }

    var discoverShowsFeed: DiscoverFeed
      get() {
        val default = DiscoverFeed.TRENDING.name
        return DiscoverFeed.valueOf(preferences.getString(DISCOVER_SHOWS_FEED, default) ?: default)
      }
      set(value) = preferences.edit { putString(DISCOVER_SHOWS_FEED, value.name) }

    var discoverShowsProviders: List<StreamingProvider>
      get() = readProviders(DISCOVER_SHOWS_PROVIDERS)
      set(value) = writeProviders(DISCOVER_SHOWS_PROVIDERS, value)

    // Movies

    var myMoviesGenres: List<Genre>
      get() {
        val filters = preferences.getStringSet(MY_MOVIES_GENRES, emptySet()) ?: emptySet()
        return filters.map { Genre.valueOf(it) }
      }
      set(value) {
        preferences.edit { putStringSet(MY_MOVIES_GENRES, value.map { it.name }.toSet()) }
      }

    var watchlistMoviesUpcoming by EnumPreference(
      preferences,
      WATCHLIST_MOVIES_UPCOMING,
      UpcomingFilter.OFF,
      UpcomingFilter::class.java,
    )
    var watchlistMoviesGenres: List<Genre>
      get() {
        val filters = preferences.getStringSet(WATCHLIST_MOVIES_GENRES, emptySet()) ?: emptySet()
        return filters.map { Genre.valueOf(it) }
      }
      set(value) {
        preferences.edit { putStringSet(WATCHLIST_MOVIES_GENRES, value.map { it.name }.toSet()) }
      }

    var hiddenMoviesGenres: List<Genre>
      get() {
        val filters = preferences.getStringSet(HIDDEN_MOVIES_GENRES, emptySet()) ?: emptySet()
        return filters.map { Genre.valueOf(it) }
      }
      set(value) {
        preferences.edit { putStringSet(HIDDEN_MOVIES_GENRES, value.map { it.name }.toSet()) }
      }

    var discoverMoviesFeed: DiscoverFeed
      get() {
        val default = DiscoverFeed.TRENDING.name
        return DiscoverFeed.valueOf(preferences.getString(DISCOVER_MOVIES_FEED, default) ?: default)
      }
      set(value) = preferences.edit { putString(DISCOVER_MOVIES_FEED, value.name) }

    var discoverMoviesProviders: List<StreamingProvider>
      get() = readProviders(DISCOVER_MOVIES_PROVIDERS)
      set(value) = writeProviders(DISCOVER_MOVIES_PROVIDERS, value)

    /**
     * A selected provider is stored whole - `id|name|logo` - rather than as a bare id, so the filter chip can name itself before the provider directory has been fetched, and still name itself with no network at all.
     *
     * Anything that no longer parses is dropped instead of throwing: the entries are region-scoped, and a preference file can outlive the region it was written in.
     */
    private fun readProviders(key: String): List<StreamingProvider> =
      preferences
        .getStringSet(key, emptySet())
        .orEmpty()
        .mapNotNull { entry ->
          val parts = entry.split(PROVIDER_SEPARATOR)
          if (parts.size < 3) return@mapNotNull null
          val id = parts.first().toLongOrNull() ?: return@mapNotNull null
          StreamingProvider(
            id = id,
            // A name holding the separator would otherwise lose its tail.
            name = parts.subList(1, parts.size - 1).joinToString(PROVIDER_SEPARATOR),
            logoPath = parts.last(),
          )
        }.sortedBy { it.name }

    private fun writeProviders(
      key: String,
      value: List<StreamingProvider>,
    ) = preferences.edit {
      putStringSet(
        key,
        value.map { "${it.id}$PROVIDER_SEPARATOR${it.name}$PROVIDER_SEPARATOR${it.logoPath}" }.toSet(),
      )
    }
  }
