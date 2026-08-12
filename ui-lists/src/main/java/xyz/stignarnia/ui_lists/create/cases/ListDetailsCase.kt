package xyz.stignarnia.ui_lists.create.cases

import xyz.stignarnia.repository.ListsRepository
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ListDetailsCase @Inject constructor(
  private val listsRepository: ListsRepository,
) {

  suspend fun loadDetails(id: Long) = listsRepository.loadById(id)
}
