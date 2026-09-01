package xyz.stignarnia.uiBase.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

/**
 * Lays an [OverscrollActionView] over a full-window [RecyclerView].
 *
 * The action's resting height matches the RecyclerView's top padding, so its extra height can move the list down without reserving a separate row or cancelling one with a negative margin.
 */
class OverscrollRecyclerLayout
  @JvmOverloads
  constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
  ) : FrameLayout(context, attrs, defStyleAttr) {
    override fun onLayout(
      changed: Boolean,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
    ) {
      super.onLayout(changed, left, top, right, bottom)

      val action = getChildAt(0) as? OverscrollActionView ?: return
      val recycler = getChildAt(1) as? RecyclerView ?: return
      recycler.offsetTopAndBottom(action.height - action.restHeight - recycler.top)
    }
  }
