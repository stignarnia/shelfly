package xyz.stignarnia.ui_base.common.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * An ImageView whose click is driven by a touch listener rather than by the framework.
 *
 * A view with a touch listener that consumes the gesture never reaches View.onTouchEvent, so the framework never performs the click and accessibility services stop being told one happened.
 * Overriding performClick is what makes the click the view's own to perform, so a listener can call it at the right moment.
 *
 * Used for the drag handle in the list detail rows, where the touch listener starts a drag.
 */
class TouchableImageView
  @JvmOverloads
  constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
  ) : AppCompatImageView(context, attrs, defStyleAttr) {
    override fun performClick(): Boolean = super.performClick()
  }
