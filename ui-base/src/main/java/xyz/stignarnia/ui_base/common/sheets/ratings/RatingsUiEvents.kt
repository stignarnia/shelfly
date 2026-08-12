@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_base.common.sheets.ratings

import xyz.stignarnia.ui_base.common.sheets.ratings.RatingsBottomSheet.Options.Operation
import xyz.stignarnia.ui_base.utilities.events.Event

data class FinishUiEvent(
  val operation: Operation,
) : Event<Operation>(operation)
