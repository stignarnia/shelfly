package xyz.stignarnia.uiShow.sections.people

import xyz.stignarnia.uiModel.Person

data class ShowDetailsPeopleUiState(
  val isLoading: Boolean = true,
  val actors: List<Person>? = null,
  val crew: Map<Person.Department, List<Person>>? = null,
)
