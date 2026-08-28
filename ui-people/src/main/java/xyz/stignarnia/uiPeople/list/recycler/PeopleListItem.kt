package xyz.stignarnia.uiPeople.list.recycler

import xyz.stignarnia.uiModel.Person

sealed class PeopleListItem {
  data class PersonItem(
    val person: Person,
  ) : PeopleListItem()

  data class HeaderItem(
    val department: Person.Department,
    val mediaTitle: String,
  ) : PeopleListItem()
}
