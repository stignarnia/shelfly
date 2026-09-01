package xyz.stignarnia.uiBase.common.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import me.everything.android.ui.overscroll.IOverScrollDecor
import me.everything.android.ui.overscroll.IOverScrollState.STATE_BOUNCE_BACK
import me.everything.android.ui.overscroll.IOverScrollState.STATE_DRAG_START_SIDE
import me.everything.android.ui.overscroll.OverScrollBounceEffectDecoratorBase
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.common.OverscrollTopAdapter
import xyz.stignarnia.uiBase.databinding.ViewOverscrollActionBinding
import xyz.stignarnia.uiBase.utilities.extensions.bump

/**
 * Pull a list past its top to trigger an action.
 *
 * The icon fades and scales in as you drag; holding past the threshold fills a ring over roughly half a second and bumps when full; releasing then fires [onTriggered].
 * The deliberate fill is what makes the gesture safe to put on a scrolling list - a stray flick cannot start anything.
 *
 * Set [actionIcon] to say what the pull will do.
 * While the action runs, call [setRunning] to keep the view up with an indeterminate spinner, which is what replaces a SwipeRefreshLayout's spinner for callers that had one.
 *
 * Host this inside an [OverscrollRecyclerLayout] above the list: the view owns its own height - matching the header gap at rest, opening as the pull progresses - so the container offsets the list below to make room.
 * Occupying a slot of its own rather than floating is what makes overlap impossible in either direction, and it costs nothing while idle because the slot matches the resting top gap.
 * Where the list runs under a floating header that scrolls away, pass that header to [follow] so the ring leaves with it instead of being stranded over the content.
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

      private const val MAX_PROGRESS = 100
      private const val FILL_DURATION_MS = 500L
      private const val HEIGHT_DURATION_MS = 200L
      private const val BUMP_DURATION_MS = 200L
    }

    private val binding =
      ViewOverscrollActionBinding
        .inflate(LayoutInflater.from(context), this)

    private var decor: IOverScrollDecor? = null
    private var recycler: RecyclerView? = null
    private var fillAnimator: ValueAnimator? = null
    private var heightAnimator: ValueAnimator? = null
    private var armed = true
    private var isRunning = false
    private var header: View? = null
    private var headerListener: ViewTreeObserver.OnPreDrawListener? = null
    private var attachedLifecycleOwner: LifecycleOwner? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null

    /**
     * How far the slot opens at a full pull.
     * Defaults to just the ring; callers whose indicator has to clear floating header views set it larger.
     */
    var openHeight: Int =
      resources.getDimensionPixelSize(R.dimen.overscrollActionProgress) +
        resources.getDimensionPixelSize(R.dimen.spaceMedium) * 2
      set(value) {
        field = value
        if (isRunning) {
          setRowHeight(value)
        }
      }

    /**
     * The slot's height at rest.
     * This replaces the list's own top padding rather than adding to it - the slot IS the gap under the floating header - so opening it only has to find the difference, and the ring ends up sitting just above the content instead of miles above it.
     */
    var restHeight: Int = 0
      set(value) {
        field = value
        if (!isRunning) {
          setRowHeight(value)
        }
      }

    /** Invoked once per completed pull. */
    var onTriggered: (() -> Unit)? = null

    init {
      alpha = 0F
      scaleX = 0F
      scaleY = 0F
      translationZ = resources.getDimension(R.dimen.overscrollActionElevation)
      context.theme
        .obtainStyledAttributes(attrs, R.styleable.OverscrollActionView, 0, 0)
        .use { typed ->
          val icon = typed.getResourceId(R.styleable.OverscrollActionView_actionIcon, 0)
          if (icon != 0) setActionIcon(icon)

          val size = typed.getDimensionPixelSize(R.styleable.OverscrollActionView_actionIconSize, 0)
          if (size > 0) {
            binding.overscrollActionIcon.updateLayoutParams {
              width = size
              height = size
            }
          }
        }
    }

    fun setActionIcon(
      @DrawableRes iconRes: Int,
    ) {
      binding.overscrollActionIcon.setImageResource(iconRes)
    }

    /** Sizes the icon inside the ring, for marks that need to sit smaller. */
    fun setActionIconSize(sizePx: Int) {
      binding.overscrollActionIcon.updateLayoutParams {
        width = sizePx
        height = sizePx
      }
    }

    /**
     * Attaches the gesture to [recycler].
     * Safe to call repeatedly; only the first call takes effect until [detach].
     */
    fun attach(
      recycler: RecyclerView,
      lifecycleOwner: LifecycleOwner? = null,
    ) {
      if (decor != null) return

      this.recycler = recycler
      decor =
        VerticalOverScrollBounceEffectDecorator(
          OverscrollTopAdapter(recycler),
          1F,
          OverScrollBounceEffectDecoratorBase.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK,
          OverScrollBounceEffectDecoratorBase.DEFAULT_DECELERATE_FACTOR,
        ).apply {
          setOverScrollUpdateListener { _, state, offset ->
            onDragUpdate(state, offset)
          }
        }

      lifecycleOwner?.let { owner ->
        attachedLifecycleOwner = owner
        val observer =
          object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
              detach()
            }
          }
        lifecycleObserver = observer
        owner.lifecycle.addObserver(observer)
      }
    }

    fun detach() {
      // Reset, or a view re-attached mid-run would refuse to show the indicator again: both running setters treat the flag as already handled.
      isRunning = false
      cancelFill()
      animate().cancel()
      heightAnimator?.cancel()
      heightAnimator = null
      setRowHeight(restHeight)
      recycler = null
      decor?.detach()
      decor = null
      header = null
      stopFollowing()
      lifecycleObserver?.let { observer ->
        attachedLifecycleOwner?.lifecycle?.removeObserver(observer)
      }
      lifecycleObserver = null
      attachedLifecycleOwner = null
    }

    /**
     * Keeps the indicator in step with a floating header that scrolls away with the list.
     *
     * The ring is placed against the header, so once the header translates out of the way the ring has to go with it or it is left painting over the content the header used to cover.
     * The slot cannot simply take the header's own CoordinatorLayout behaviour: it lives inside the stack above the list rather than directly under the coordinator, and a behaviour only ever runs on a direct child.
     * Nor can the header's translation be observed - the behaviour animates it through a ViewPropertyAnimator, which writes the render node without going through setTranslationY - so it is read once a frame instead, which catches the drag and the snap back to the top alike.
     *
     * Only the translation is copied.
     * The slot keeps its own height, so nothing about this moves the list.
     */
    fun follow(view: View) {
      header = view
      if (isAttachedToWindow) startFollowing()
    }

    override fun onAttachedToWindow() {
      super.onAttachedToWindow()
      if (header != null) startFollowing()
    }

    override fun onDetachedFromWindow() {
      cancelFill()
      animate().cancel()
      heightAnimator?.cancel()
      stopFollowing()
      super.onDetachedFromWindow()
    }

    private fun startFollowing() {
      if (headerListener != null) return
      headerListener =
        ViewTreeObserver
          .OnPreDrawListener {
            header?.let {
              if (translationY != it.translationY) {
                translationY = it.translationY
              }
            }
            true
          }.also { viewTreeObserver.addOnPreDrawListener(it) }
    }

    private fun stopFollowing() {
      headerListener?.let { listener ->
        val vto = if (viewTreeObserver.isAlive) viewTreeObserver else getViewTreeObserver()
        if (vto.isAlive) {
          vto.removeOnPreDrawListener(listener)
        }
      }
      headerListener = null
      translationY = 0F
    }

    /**
     * Keeps the indicator on screen with an indeterminate spinner while the triggered action is still working.
     */
    fun setRunning(running: Boolean) {
      if (isRunning == running) return
      isRunning = running

      binding.overscrollActionProgress.isIndeterminate = running
      if (!running) binding.overscrollActionProgress.setProgressCompat(0, false)
      animateIndicator(visible = running)
    }

    /**
     * Keeps the indicator up while the triggered action runs, with the ring filled to [percent] of the work actually done.
     * Pass null once the action has finished, which puts the indicator away.
     *
     * Preferred over [setRunning] by callers that can measure their work: the spinner says only that something is happening, this says how much is left.
     * The two are mutually exclusive - a caller uses one or the other.
     */
    fun setRunningProgress(percent: Int?) {
      if (percent == null) {
        if (!isRunning) return
        isRunning = false
        binding.overscrollActionProgress.setProgressCompat(0, false)
        animateIndicator(visible = false)
        return
      }

      val wasRunning = isRunning
      if (!wasRunning) {
        isRunning = true
        // The pull's own fill is sitting full, from the hold that armed the trigger.
        // Drop it to zero unanimated before taking over, or the first real reading is seen as the ring draining backwards from full.
        cancelFill()
        binding.overscrollActionProgress.isIndeterminate = false
        binding.overscrollActionProgress.setProgressCompat(0, false)
        animateIndicator(visible = true)
      }
      // Animated only once the ring is already up and showing a real reading; the opening frame would otherwise animate away from a zero the user never saw.
      binding.overscrollActionProgress
        .setProgressCompat(percent.coerceIn(0, MAX_PROGRESS), wasRunning)
    }

    private fun animateIndicator(visible: Boolean) {
      val to = if (visible) 1F else 0F
      animate()
        .alpha(to)
        .scaleX(to)
        .scaleY(to)
        .setDuration(BUMP_DURATION_MS)
        .start()
      animateRowHeight(if (visible) openHeight else restHeight)
    }

    /**
     * The row is only ever resized, never translated, so the list below simply moves to make room.
     * Height is driven directly rather than through an adapter notification, which would rebind on every frame of a drag.
     */
    private fun setRowHeight(px: Int) {
      // Never below the resting gap, and nothing outside this view is touched: the slot's height is the only thing that ever moves the list, so when it comes back to rest the list is exactly where it started.
      val height = px.coerceAtLeast(restHeight)
      val lp = layoutParams ?: return
      if (lp.height != height) {
        lp.height = height
        layoutParams = lp
      }
    }

    private fun animateRowHeight(to: Int) {
      heightAnimator?.cancel()
      val from = layoutParams?.height ?: 0
      if (from == to) return
      heightAnimator =
        ValueAnimator.ofInt(from, to).apply {
          duration = HEIGHT_DURATION_MS
          addUpdateListener { setRowHeight(it.animatedValue as Int) }
          start()
        }
    }

    private fun onDragUpdate(
      state: Int,
      offset: Float,
    ) {
      // Running the action owns the indicator; a drag must not fight it.
      if (isRunning) return

      if (offset <= 0) {
        alpha = 0F
        scaleX = 0F
        scaleY = 0F
        setRowHeight(restHeight)
        cancelFill()
        return
      }

      val value = (offset / OVERSCROLL_OFFSET).coerceAtMost(1F)
      // Follow the drag rather than snapping: it is only the difference between the resting gap and the open one, so the ring barely travels.
      setRowHeight(restHeight + ((openHeight - restHeight) * value).toInt())
      alpha = value
      scaleX = value
      scaleY = value

      if (value >= 1F) startFill() else cancelFill()

      when (state) {
        STATE_DRAG_START_SIDE -> {
          armed = true
        }

        STATE_BOUNCE_BACK -> {
          val filled = binding.overscrollActionProgress.progress >= MAX_PROGRESS
          if (offset >= OVERSCROLL_OFFSET && armed && filled) {
            armed = false
            onTriggered?.invoke()
          }
        }
      }
    }

    private fun startFill() {
      if (fillAnimator != null) return
      fillAnimator =
        ValueAnimator.ofInt(0, MAX_PROGRESS).apply {
          duration = FILL_DURATION_MS
          interpolator = LinearInterpolator()
          addUpdateListener { animator ->
            val progress = animator.animatedValue as Int
            binding.overscrollActionProgress.progress = progress
            if (progress >= MAX_PROGRESS) {
              bump(BUMP_DURATION_MS)
            }
          }
          start()
        }
    }

    private fun cancelFill() {
      fillAnimator?.cancel()
      fillAnimator = null
      binding.overscrollActionProgress.progress = 0
    }
  }
