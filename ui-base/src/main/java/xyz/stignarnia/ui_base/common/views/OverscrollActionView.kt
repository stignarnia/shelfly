package xyz.stignarnia.ui_base.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.IOverScrollDecor
import me.everything.android.ui.overscroll.IOverScrollState.STATE_BOUNCE_BACK
import me.everything.android.ui.overscroll.IOverScrollState.STATE_DRAG_START_SIDE
import me.everything.android.ui.overscroll.OverScrollBounceEffectDecoratorBase
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.common.OverscrollTopAdapter
import xyz.stignarnia.ui_base.databinding.ViewOverscrollActionBinding
import xyz.stignarnia.ui_base.utilities.extensions.bump

/**
 * Pull a list past its top to trigger an action.
 *
 * The icon fades and scales in as you drag; holding past the threshold fills a
 * ring over roughly half a second and bumps when full; releasing then fires
 * [onTriggered]. The deliberate fill is what makes the gesture safe to put on a
 * scrolling list - a stray flick cannot start anything.
 *
 * Set [actionIcon] to say what the pull will do. While the action runs, call
 * [setRunning] to keep the view up with an indeterminate spinner, which is what
 * replaces a SwipeRefreshLayout's spinner for callers that had one.
 */
class OverscrollActionView
  @JvmOverloads
  constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
  ) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
      /** Drag distance, in pixels, at which the ring starts filling. */
      private const val OVERSCROLL_OFFSET = 225F

      /** Divisor turning drag distance into the icon's downward travel. */
      private const val OVERSCROLL_OFFSET_TRANSLATION = 4.5F

      private const val FILL_STEPS = 100
      private const val FILL_STEP_DELAY_MS = 5L
      private const val BUMP_DURATION_MS = 200L
    }

    private val binding = ViewOverscrollActionBinding
      .inflate(LayoutInflater.from(context), this)

    private var decor: IOverScrollDecor? = null
    private var fillJob: Job? = null
    private var armed = true
    private var isRunning = false

    /** Invoked once per completed pull. */
    var onTriggered: (() -> Unit)? = null

    init {
      alpha = 0F
      scaleX = 0F
      scaleY = 0F
      context.theme
        .obtainStyledAttributes(attrs, R.styleable.OverscrollActionView, 0, 0)
        .use { typed ->
          val icon = typed.getResourceId(R.styleable.OverscrollActionView_actionIcon, 0)
          if (icon != 0) setActionIcon(icon)
        }
    }

    fun setActionIcon(
      @DrawableRes iconRes: Int,
    ) {
      binding.overscrollActionIcon.setImageResource(iconRes)
    }

    /**
     * Attaches the gesture to [recycler]. Safe to call repeatedly; only the
     * first call takes effect until [detach].
     */
    fun attach(
      recycler: RecyclerView,
      lifecycleOwner: LifecycleOwner,
    ) {
      if (decor != null) return

      decor = VerticalOverScrollBounceEffectDecorator(
        OverscrollTopAdapter(recycler),
        1F,
        OverScrollBounceEffectDecoratorBase.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK,
        OverScrollBounceEffectDecoratorBase.DEFAULT_DECELERATE_FACTOR,
      ).apply {
        setOverScrollUpdateListener { _, state, offset ->
          onDragUpdate(state, offset, lifecycleOwner)
        }
      }
    }

    fun detach() {
      fillJob?.cancel()
      fillJob = null
      decor?.detach()
      decor = null
    }

    /**
     * Keeps the indicator on screen with an indeterminate spinner while the
     * triggered action is still working.
     */
    fun setRunning(running: Boolean) {
      if (isRunning == running) return
      isRunning = running

      binding.overscrollActionProgress.isIndeterminate = running
      if (running) {
        animate()
          .alpha(1F)
          .scaleX(1F)
          .scaleY(1F)
          .setDuration(BUMP_DURATION_MS)
          .start()
      } else {
        binding.overscrollActionProgress.progress = 0
        animate()
          .alpha(0F)
          .scaleX(0F)
          .scaleY(0F)
          .setDuration(BUMP_DURATION_MS)
          .start()
      }
    }

    private fun onDragUpdate(
      state: Int,
      offset: Float,
      lifecycleOwner: LifecycleOwner,
    ) {
      // Running the action owns the indicator; a drag must not fight it.
      if (isRunning) return

      if (offset <= 0) {
        alpha = 0F
        scaleX = 0F
        scaleY = 0F
        translationY = 0F
        cancelFill()
        return
      }

      val value = (offset / OVERSCROLL_OFFSET).coerceAtMost(1F)
      alpha = value
      scaleX = value
      scaleY = value
      translationY = offset / OVERSCROLL_OFFSET_TRANSLATION

      if (value >= 1F) startFill(lifecycleOwner) else cancelFill()

      when (state) {
        STATE_DRAG_START_SIDE -> armed = true
        STATE_BOUNCE_BACK -> {
          val filled = binding.overscrollActionProgress.progress >= FILL_STEPS
          if (offset >= OVERSCROLL_OFFSET && armed && filled) {
            armed = false
            onTriggered?.invoke()
          }
        }
      }
    }

    private fun startFill(lifecycleOwner: LifecycleOwner) {
      if (fillJob != null) return
      fillJob = lifecycleOwner.lifecycleScope.launch {
        repeat(FILL_STEPS) { step ->
          val progress = step + 1
          binding.overscrollActionProgress.progress = progress
          if (progress >= FILL_STEPS) {
            bump(BUMP_DURATION_MS)
          }
          delay(FILL_STEP_DELAY_MS)
        }
      }
    }

    private fun cancelFill() {
      fillJob?.cancel()
      fillJob = null
      binding.overscrollActionProgress.progress = 0
    }
  }
