
package xyz.stignarnia.uiShow

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiPeople.details.PersonDetailsArgs

sealed class ShowDetailsEvent<T>(
  action: T,
) : Event<T>(action) {
  data class OpenPersonSheet(
    val show: Show,
    val person: Person,
    val personArgs: PersonDetailsArgs?,
  ) : ShowDetailsEvent<Show>(show)

  data class OpenPeopleSheet(
    val show: Show,
    val people: List<Person>,
    val department: Person.Department,
  ) : ShowDetailsEvent<Show>(show)

  data object RefreshSeasons : ShowDetailsEvent<Unit>(Unit)

  data object Finish : ShowDetailsEvent<Unit>(Unit)
}
