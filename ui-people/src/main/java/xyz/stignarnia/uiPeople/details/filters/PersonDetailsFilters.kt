package xyz.stignarnia.uiPeople.details.filters

import xyz.stignarnia.common.Mode

data class PersonDetailsFilters(
  val modes: List<Mode> = emptyList(),
  val onlyCollection: Boolean = false,
)
