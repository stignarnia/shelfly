package xyz.stignarnia.uiMovie.sections.collections.list

import xyz.stignarnia.repository.movies.MovieCollectionsRepository.Source
import xyz.stignarnia.uiModel.MovieCollection

data class MovieDetailsCollectionsUiState(
  val isLoading: Boolean = true,
  val collections: Pair<List<MovieCollection>, Source>? = null,
)
