package xyz.stignarnia.ui_base.common.views.modal

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.databinding.ViewModalBinding
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.fadeOut
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.screenHeight

/**
 * The app's only modal. It began as the "?" tip popup and still is one: a card
 * on a translucent scrim that springs up from a third of a screen down, inside
 * the host's view hierarchy rather than in a window of its own.
 *
 * Build one with [ModalBuilder] instead of configuring it directly.
 */
class ModalView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  internal val binding = ViewModalBinding.inflate(LayoutInflater.from(context), this)

  /** Runs once the modal has faded out and left the hierarchy. */
  var onDismissed: (() -> Unit)? = null

  private var isDismissing = false

  private var backCallback: OnBackPressedCallback? = null

  init {
    layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    setBackgroundResource(R.color.colorBlackTranslucent)
    binding.modalMessage.movementMethod = LinkMovementMethod.getInstance()
    // Only the scrim gets this: the card is clickable in the layout, so it
    // swallows its own taps rather than letting them fall through to here.
    onClick { dismiss() }
  }

  /**
   * Back has to be claimed on attach rather than left to the host. Fragments
   * register their own callback on every onResume, and the dispatcher runs the
   * most recently added one first, so anything the host installs up front is
   * already buried by the time a modal opens.
   */
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    val dispatcher = findViewTreeOnBackPressedDispatcherOwner()?.onBackPressedDispatcher ?: return
    backCallback = dispatcher.addCallback { dismiss() }
  }

  override fun onDetachedFromWindow() {
    backCallback?.remove()
    backCallback = null
    super.onDetachedFromWindow()
  }

  private val springStartValue by lazy { (screenHeight().toFloat()) / 3F }
  private val springAnimation by lazy {
    SpringAnimation(binding.modalCard, DynamicAnimation.TRANSLATION_Y, 0F).apply {
      spring.stiffness = 300F
      spring.dampingRatio = 0.65F
      setStartValue(springStartValue)
    }
  }

  /** For a neutral action that has to stand down while it is already running. */
  fun setNeutralButtonEnabled(enabled: Boolean) {
    binding.modalNeutralButton.isEnabled = enabled
  }

  internal fun show() {
    springAnimation.setStartValue(springStartValue)
    springAnimation.start()
    fadeIn()
  }

  /**
   * Removes the modal once the fade finishes. Guarded because a button and the
   * back press can both reach it, and the second call would fire [onDismissed]
   * again after the view has already gone.
   */
  fun dismiss() {
    if (isDismissing) return
    isDismissing = true
    fadeOut {
      (parent as? ViewGroup)?.removeView(this)
      onDismissed?.invoke()
    }
  }
}
