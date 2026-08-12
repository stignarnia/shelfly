package xyz.stignarnia.ui_movie.sections.people

import xyz.stignarnia.ui_model.Person

data class MovieDetailsPeopleUiState(
  val isLoading: Boolean = true,
  val actors: List<Person>? = null,
  val crew: Map<Person.Department, List<Person>>? = null,
)
