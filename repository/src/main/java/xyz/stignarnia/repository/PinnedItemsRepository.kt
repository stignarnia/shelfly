package xyz.stignarnia.repository

import android.content.SharedPreferences
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Show
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PinnedItemsRepository @Inject constructor(
  @param:Named("watchlistPreferences") private val sharedPreferences: SharedPreferences,
  @param:Named("progressMoviesPreferences") private val sharedPreferencesMovies: SharedPreferences,
) {

  fun addPinnedItem(show: Show) = addShowPinnedItem(IdTmdb(show.tmdbId))

  fun addPinnedItem(movie: Movie) = addMoviePinnedItem(IdTmdb(movie.tmdbId))

  fun addShowPinnedItem(showId: IdTmdb) = sharedPreferences.edit().putLong(showId.id.toString(), showId.id).apply()

  fun addMoviePinnedItem(movieId: IdTmdb) =
    sharedPreferencesMovies.edit().putLong(movieId.id.toString(), movieId.id).apply()

  fun removePinnedItem(show: Show) = removeShowPinnedItem(IdTmdb(show.tmdbId))

  fun removePinnedItem(movie: Movie) = removeMoviePinnedItem(IdTmdb(movie.tmdbId))

  fun removeShowPinnedItem(showId: IdTmdb) = sharedPreferences.edit().remove(showId.id.toString()).apply()

  fun removeMoviePinnedItem(movieId: IdTmdb) = sharedPreferencesMovies.edit().remove(movieId.id.toString()).apply()

  fun isItemPinned(show: Show) = sharedPreferences.contains(show.tmdbId.toString())

  fun isItemPinned(movie: Movie) = sharedPreferencesMovies.contains(movie.tmdbId.toString())

  fun getAllMovies(): List<Long> = sharedPreferencesMovies.all.values.map { it as Long }

  fun getAllShows(): List<Long> = sharedPreferences.all.values.map { it as Long }
}
