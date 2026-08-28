package xyz.stignarnia.uiPeople.list

import xyz.stignarnia.uiPeople.list.recycler.PeopleListItem

data class PeopleListUiState(
  val peopleItems: List<PeopleListItem>? = null,
)
