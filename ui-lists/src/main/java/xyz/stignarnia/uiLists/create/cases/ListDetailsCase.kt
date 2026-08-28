package xyz.stignarnia.uiLists.create.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.ListsRepository
import javax.inject.Inject

@ViewModelScoped
class ListDetailsCase
  @Inject
  constructor(
    private val listsRepository: ListsRepository,
  ) {
    suspend fun loadDetails(id: Long) = listsRepository.loadById(id)
  }
