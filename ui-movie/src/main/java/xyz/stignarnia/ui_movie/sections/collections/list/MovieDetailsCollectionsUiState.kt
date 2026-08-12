package xyz.stignarnia.ui_movie.sections.collections.list

import xyz.stignarnia.repository.movies.MovieCollectionsRepository.Source
import xyz.stignarnia.ui_model.MovieCollection

data class MovieDetailsCollectionsUiState(
  val isLoading: Boolean = true,
  val collections: Pair<List<MovieCollection>, Source>? = null,
)
