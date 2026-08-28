package xyz.stignarnia.uiBase.common.sheets.contextMenu.show

import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.helpers.ShowContextItem

data class ShowContextMenuUiState(
  val isLoading: Boolean? = null,
  val isLoadingSecondary: Boolean? = null,
  val item: ShowContextItem? = null,
)
