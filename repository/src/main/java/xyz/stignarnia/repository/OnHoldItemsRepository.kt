package xyz.stignarnia.repository

import android.content.SharedPreferences
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Show
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OnHoldItemsRepository @Inject constructor(
  @param:Named("progressOnHoldPreferences") private val sharedPreferences: SharedPreferences,
) {

  fun getAll(): List<IdTmdb> = sharedPreferences.all.keys.map { IdTmdb(it.toLong()) }

  fun addItem(show: Show) = addItem(IdTmdb(show.tmdbId))

  fun addItem(showId: IdTmdb) = sharedPreferences.edit().putLong(showId.id.toString(), showId.id).apply()

  fun removeItem(show: Show) = removeItem(IdTmdb(show.tmdbId))

  fun removeItem(showId: IdTmdb) = sharedPreferences.edit().remove(showId.id.toString()).apply()

  fun isOnHold(show: Show) = sharedPreferences.contains(show.tmdbId.toString())
}
