package com.michaldrabik.repository

import android.content.SharedPreferences
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Show
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OnHoldItemsRepository @Inject constructor(
  @Named("progressOnHoldPreferences") private val sharedPreferences: SharedPreferences,
) {

  fun getAll(): List<IdTmdb> = sharedPreferences.all.keys.map { IdTmdb(it.toLong()) }

  fun addItem(show: Show) = addItem(IdTmdb(show.tmdbId))

  fun addItem(showId: IdTmdb) = sharedPreferences.edit().putLong(showId.id.toString(), showId.id).apply()

  fun removeItem(show: Show) = sharedPreferences.edit().remove(show.tmdbId.toString()).apply()

  fun isOnHold(show: Show) = sharedPreferences.contains(show.tmdbId.toString())
}
