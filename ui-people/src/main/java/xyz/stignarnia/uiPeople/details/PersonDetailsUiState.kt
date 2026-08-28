package xyz.stignarnia.uiPeople.details

import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsItem

data class PersonDetailsUiState(
  val personDetailsItems: List<PersonDetailsItem>? = null,
)
