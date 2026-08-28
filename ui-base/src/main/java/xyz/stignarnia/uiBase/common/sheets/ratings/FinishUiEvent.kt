
package xyz.stignarnia.uiBase.common.sheets.ratings

import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet.Options.Operation
import xyz.stignarnia.uiBase.utilities.events.Event

data class FinishUiEvent(
  val operation: Operation,
) : Event<Operation>(operation)
