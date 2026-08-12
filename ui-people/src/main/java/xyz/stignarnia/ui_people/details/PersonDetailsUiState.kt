package xyz.stignarnia.ui_people.details

import xyz.stignarnia.ui_people.details.recycler.PersonDetailsItem

data class PersonDetailsUiState(
  val personDetailsItems: List<PersonDetailsItem>? = null,
)
