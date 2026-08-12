package com.michaldrabik.ui_lists.create.cases

import com.michaldrabik.repository.ListsRepository
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.repository.settings.SettingsRepository
import com.michaldrabik.ui_base.events.EventsManager
import com.michaldrabik.ui_model.CustomList
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class CreateListCase @Inject constructor(
  private val mappers: Mappers,
  private val listsRepository: ListsRepository,
  private val settingsRepository: SettingsRepository,
  private val eventsManager: EventsManager,
) {

  suspend fun createList(
    name: String,
    description: String?,
  ): CustomList = listsRepository.createList(name, description, null, null)

  suspend fun updateList(list: CustomList): CustomList =
    listsRepository.updateList(list.id, list.idTmdb, list.idSlug, list.name, list.description)
}
