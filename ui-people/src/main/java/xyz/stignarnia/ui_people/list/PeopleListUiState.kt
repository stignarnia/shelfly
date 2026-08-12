package xyz.stignarnia.ui_people.list

import xyz.stignarnia.ui_people.list.recycler.PeopleListItem

data class PeopleListUiState(
  val peopleItems: List<PeopleListItem>? = null,
)
