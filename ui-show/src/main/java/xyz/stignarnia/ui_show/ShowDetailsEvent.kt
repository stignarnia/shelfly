
package xyz.stignarnia.ui_show

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.Person
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_people.details.PersonDetailsArgs

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
