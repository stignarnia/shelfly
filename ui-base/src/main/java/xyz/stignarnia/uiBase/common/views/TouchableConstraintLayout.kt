package xyz.stignarnia.uiBase.common.views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * A ConstraintLayout whose click is driven by a touch listener rather than by the framework.
 *
 * See [TouchableImageView] for why overriding performClick is the part that matters.
 * Used for the row body in the list detail rows, where the touch listener consumes the gesture once it becomes a horizontal swipe and so has to perform the tap itself.
 */
class TouchableConstraintLayout
  @JvmOverloads
  constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
  ) : ConstraintLayout(context, attrs, defStyleAttr) {
    override fun performClick(): Boolean = super.performClick()
  }
