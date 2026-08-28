package xyz.stignarnia.uiShow.sections.streamings

import xyz.stignarnia.uiModel.StreamingService

data class ShowDetailsStreamingsUiState(
  val streamings: StreamingsState? = null,
) {
  data class StreamingsState(
    val streamings: List<StreamingService>,
    val isLocal: Boolean,
  )
}
