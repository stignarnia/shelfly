package xyz.stignarnia.uiMovie.sections.streamings

import xyz.stignarnia.uiModel.StreamingService

data class MovieDetailsStreamingsUiState(
  val streamings: StreamingsState? = null,
) {
  data class StreamingsState(
    val streamings: List<StreamingService>,
    val isLocal: Boolean,
  )
}
