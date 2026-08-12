package xyz.stignarnia.ui_people.list.cases

import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PeopleRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Person
import xyz.stignarnia.ui_people.list.recycler.PeopleListItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class PeopleListItemsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val peopleRepository: PeopleRepository,
) {

  suspend fun loadPeople(
    idTmdb: IdTmdb,
    mode: Mode,
    department: Person.Department,
  ): List<PeopleListItem.PersonItem> =
    withContext(dispatchers.IO) {
      val ids = Ids.EMPTY.copy(tmdb = idTmdb)
      val people: Map<Person.Department, List<Person>> = when (mode) {
        Mode.SHOWS -> peopleRepository.loadAllForShow(ids)
        Mode.MOVIES -> peopleRepository.loadAllForMovie(ids)
      }
      people.getOrDefault(department, emptyList()).map {
        PeopleListItem.PersonItem(it)
      }
    }
}
