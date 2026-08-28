package xyz.stignarnia.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OnHoldItemsRepository
  @Inject
  constructor(
    @param:Named("progressOnHoldPreferences") private val sharedPreferences: SharedPreferences,
  ) {
    fun getAll(): List<IdTmdb> = sharedPreferences.all.keys.map { IdTmdb(it.toLong()) }

    fun addItem(show: Show) = addItem(IdTmdb(show.tmdbId))

    fun addItem(showId: IdTmdb) = sharedPreferences.edit { putLong(showId.id.toString(), showId.id) }

    fun removeItem(show: Show) = removeItem(IdTmdb(show.tmdbId))

    fun removeItem(showId: IdTmdb) = sharedPreferences.edit { remove(showId.id.toString()) }

    fun isOnHold(show: Show) = sharedPreferences.contains(show.tmdbId.toString())
  }
