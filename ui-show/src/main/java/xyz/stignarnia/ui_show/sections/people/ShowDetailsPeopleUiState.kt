package xyz.stignarnia.ui_show.sections.people

import xyz.stignarnia.ui_model.Person

data class ShowDetailsPeopleUiState(
  val isLoading: Boolean = true,
  val actors: List<Person>? = null,
  val crew: Map<Person.Department, List<Person>>? = null,
)
