package xyz.stignarnia.uiMovie.sections.people

import xyz.stignarnia.uiModel.Person

data class MovieDetailsPeopleUiState(
  val isLoading: Boolean = true,
  val actors: List<Person>? = null,
  val crew: Map<Person.Department, List<Person>>? = null,
)
