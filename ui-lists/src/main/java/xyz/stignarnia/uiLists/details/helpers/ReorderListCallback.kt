package xyz.stignarnia.uiLists.details.helpers

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiLists.R
import kotlin.math.max
import kotlin.math.roundToInt

class ReorderListCallback(
  context: Context,
  private val adapter: ReorderListCallbackAdapter,
) : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, START) {
  companion object {
    private const val SWIPE_ARM_FRACTION = 0.75F
    private const val SWIPE_RETREAT_TOLERANCE_DP = 8F
    private const val UNARMED_BACKGROUND_MAX_ALPHA = 0.5F
    private const val UNARMED_ICON_MAX_ALPHA = 0.5F
    private const val ARMED_ICON_SCALE = 1.3F
    private const val ICON_SCALE_DURATION_MS = 200L
  }

  private val swipeRetreatTolerance = SWIPE_RETREAT_TOLERANCE_DP * context.resources.displayMetrics.density
  private val deleteBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.colorError) }
  private val deleteBackgroundRect = RectF()
  private val deleteBackgroundCorner = context.dimenToPx(R.dimen.mediaTileCorner).toFloat()

  // Mutated because the drawable is shared with the delete action in the toolbar menu, and its alpha and bounds change on every frame here.
  private val deleteIcon = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_delete)).mutate()
  private val deleteIconMargin = context.dimenToPx(R.dimen.spaceBig)
  private var deleteIconScale = 1F
  private var deleteIconAnimator: ValueAnimator? = null

  // Distances are measured towards the start edge, so they are positive for a swipe in the allowed direction regardless of layout direction.
  private var swipeReach = 0F
  private var swipeMaxReach = 0F
  private var isSwipeArmed = false

  override fun onMove(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    target: RecyclerView.ViewHolder,
  ): Boolean {
    adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
    return true
  }

  override fun onSwiped(
    viewHolder: RecyclerView.ViewHolder,
    direction: Int,
  ) {
    adapter.onItemSwiped(viewHolder)
  }

  override fun onSelectedChanged(
    viewHolder: RecyclerView.ViewHolder?,
    actionState: Int,
  ) {
    super.onSelectedChanged(viewHolder, actionState)
    if (actionState == ACTION_STATE_SWIPE) {
      resetSwipe()
    }
  }

  override fun onChildDraw(
    c: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean,
  ) {
    if (actionState == ACTION_STATE_SWIPE) {
      val isRtl = recyclerView.layoutDirection == View.LAYOUT_DIRECTION_RTL
      val reach = if (isRtl) dX else -dX
      val armReach = recyclerView.width * SWIPE_ARM_FRACTION

      // Only the finger's own movement counts; the recover and dismiss animations also draw through here.
      if (isCurrentlyActive) {
        swipeReach = reach
        swipeMaxReach = max(swipeMaxReach, reach)
        val isPastThreshold = reach >= armReach
        if (isPastThreshold != isSwipeArmed) {
          setSwipeArmed(recyclerView, isPastThreshold)
          viewHolder.itemView.performHapticFeedback(if (isPastThreshold) armFeedback() else disarmFeedback())
        }
      }

      drawDeleteBackground(c, viewHolder.itemView, isRtl, reach / armReach)
    }
    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
  }

  // ItemTouchHelper consults this only on release, so it is where the decision to delete is made.
  // The item goes only if it is past the threshold and the finger has not drawn back from the furthest point it reached.
  // A release that keeps the item disarms it, so the row does not snap back over a background that still says it is being deleted.
  override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
    val isAtFurthestReach = swipeReach >= swipeMaxReach - swipeRetreatTolerance
    if (isSwipeArmed && isAtFurthestReach) return 0F
    if (isSwipeArmed) setSwipeArmed(viewHolder.itemView.parent as? View, false)
    return Float.MAX_VALUE
  }

  // A fling never deletes, however fast.
  override fun getSwipeEscapeVelocity(defaultValue: Float) = Float.MAX_VALUE

  override fun clearView(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
  ) {
    super.clearView(recyclerView, viewHolder)
    resetSwipe()
    adapter.onItemCleared()
  }

  override fun isItemViewSwipeEnabled() = false

  override fun isLongPressDragEnabled() = false

  private fun resetSwipe() {
    deleteIconAnimator?.cancel()
    deleteIconAnimator = null
    deleteIconScale = 1F
    swipeReach = 0F
    swipeMaxReach = 0F
    isSwipeArmed = false
  }

  // The icon's pop runs on its own clock rather than the finger's, so it has to invalidate the list itself to be drawn between touch events.
  private fun setSwipeArmed(
    recyclerView: View?,
    isArmed: Boolean,
  ) {
    isSwipeArmed = isArmed
    deleteIconAnimator?.cancel()
    deleteIconAnimator =
      ValueAnimator.ofFloat(deleteIconScale, if (isArmed) ARMED_ICON_SCALE else 1F).apply {
        duration = ICON_SCALE_DURATION_MS
        interpolator = OvershootInterpolator()
        addUpdateListener {
          deleteIconScale = it.animatedValue as Float
          recyclerView?.invalidate()
        }
        start()
      }
  }

  // Drawn across the whole row, which is opaque, so only the part the row has slid off is visible.
  private fun drawDeleteBackground(
    c: Canvas,
    itemView: View,
    isRtl: Boolean,
    progress: Float,
  ) {
    if (progress <= 0F) return
    val fade = progress.coerceAtMost(1F)

    deleteBackgroundPaint.alpha = alphaOf(if (isSwipeArmed) 1F else fade * UNARMED_BACKGROUND_MAX_ALPHA)
    deleteBackgroundRect.set(itemView.left.toFloat(), itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
    c.drawRoundRect(deleteBackgroundRect, deleteBackgroundCorner, deleteBackgroundCorner, deleteBackgroundPaint)

    val halfWidth = (deleteIcon.intrinsicWidth * deleteIconScale / 2).roundToInt()
    val halfHeight = (deleteIcon.intrinsicHeight * deleteIconScale / 2).roundToInt()
    val baseHalfWidth = deleteIcon.intrinsicWidth / 2
    val centerX = if (isRtl) itemView.left + deleteIconMargin + baseHalfWidth else itemView.right - deleteIconMargin - baseHalfWidth
    val centerY = (itemView.top + itemView.bottom) / 2
    deleteIcon.setBounds(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
    deleteIcon.alpha = alphaOf(if (isSwipeArmed) 1F else fade * UNARMED_ICON_MAX_ALPHA)
    deleteIcon.draw(c)
  }

  private fun alphaOf(fraction: Float) = (fraction * 255).roundToInt()

  private fun armFeedback() =
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CONFIRM
      else -> HapticFeedbackConstants.LONG_PRESS
    }

  private fun disarmFeedback() =
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
      else -> HapticFeedbackConstants.CLOCK_TICK
    }
}
