package xyz.stignarnia.uiLists.create.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.ListsRepository
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.events.EventsManager
import xyz.stignarnia.uiModel.CustomList
import javax.inject.Inject

@ViewModelScoped
class CreateListCase
  @Inject
  constructor(
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
