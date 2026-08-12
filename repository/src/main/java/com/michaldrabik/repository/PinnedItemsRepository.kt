package com.michaldrabik.repository

import android.content.SharedPreferences
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_model.Show
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PinnedItemsRepository @Inject constructor(
  @Named("watchlistPreferences") private val sharedPreferences: SharedPreferences,
  @Named("progressMoviesPreferences") private val sharedPreferencesMovies: SharedPreferences,
) {

  fun addPinnedItem(show: Show) = addShowPinnedItem(IdTmdb(show.tmdbId))

  fun addPinnedItem(movie: Movie) = addMoviePinnedItem(IdTmdb(movie.tmdbId))

  fun addShowPinnedItem(showId: IdTmdb) = sharedPreferences.edit().putLong(showId.id.toString(), showId.id).apply()

  fun addMoviePinnedItem(movieId: IdTmdb) =
    sharedPreferencesMovies.edit().putLong(movieId.id.toString(), movieId.id).apply()

  fun removePinnedItem(show: Show) = sharedPreferences.edit().remove(show.tmdbId.toString()).apply()

  fun removePinnedItem(movie: Movie) = sharedPreferencesMovies.edit().remove(movie.tmdbId.toString()).apply()

  fun isItemPinned(show: Show) = sharedPreferences.contains(show.tmdbId.toString())

  fun isItemPinned(movie: Movie) = sharedPreferencesMovies.contains(movie.tmdbId.toString())

  fun getAllMovies(): List<Long> = sharedPreferencesMovies.all.values.map { it as Long }

  fun getAllShows(): List<Long> = sharedPreferences.all.values.map { it as Long }
}
