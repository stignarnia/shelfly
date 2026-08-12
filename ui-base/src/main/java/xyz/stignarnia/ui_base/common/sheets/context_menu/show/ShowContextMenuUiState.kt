package xyz.stignarnia.ui_base.common.sheets.context_menu.show

import xyz.stignarnia.ui_base.common.sheets.context_menu.show.helpers.ShowContextItem

data class ShowContextMenuUiState(
  val isLoading: Boolean? = null,
  val isLoadingSecondary: Boolean? = null,
  val item: ShowContextItem? = null,
)
