package xyz.stignarnia.ui_base

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.view.ViewPropertyAnimator
import androidx.activity.addCallback
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import xyz.stignarnia.common.Mode
import xyz.stignarnia.ui_base.utilities.ModeHost
import xyz.stignarnia.ui_base.utilities.MoviesStatusHost
import xyz.stignarnia.ui_base.utilities.NavigationHost
import xyz.stignarnia.ui_base.utilities.SnackbarHost
import xyz.stignarnia.ui_base.utilities.TipsHost
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.isTablet
import xyz.stignarnia.ui_base.utilities.extensions.showErrorSnackbar
import xyz.stignarnia.ui_base.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.ui_model.Tip

abstract class BaseFragment<T : ViewModel>(
  @LayoutRes contentLayoutId: Int,
) : Fragment(contentLayoutId),
  TipsHost {

  protected abstract val viewModel: T
  open val navigationId: Int = 0

  protected var isInitialized = false

  protected val animations = mutableListOf<ViewPropertyAnimator?>()
  protected val animators = mutableListOf<Animator?>()
  protected val snackbars = mutableListOf<Snackbar?>()

  protected var mode: Mode
    get() = (requireActivity() as ModeHost).getMode()
    set(value) = (requireActivity() as ModeHost).setMode(value)

  protected val moviesEnabled: Boolean
    get() = (requireActivity() as MoviesStatusHost).hasMoviesEnabled()

  protected val isTablet by lazy { requireContext().isTablet() }

  override fun onResume() {
    super.onResume()
    setupBackPressed()
  }

  protected fun findNavControl() = (requireActivity() as NavigationHost).findNavControl()

  protected fun hideNavigation(animate: Boolean = true) = (requireActivity() as NavigationHost).hideNavigation(animate)

  protected fun showNavigation(animate: Boolean = true) = (requireActivity() as NavigationHost).showNavigation(animate)

  protected fun showSnack(message: MessageEvent) {
    val host = (requireActivity() as SnackbarHost).provideSnackbarLayout()
    when (message) {
      is MessageEvent.Info -> {
        val length = if (message.isIndefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_SHORT
        val action = if (message.isIndefinite) ({}) else null
        host.showInfoSnackbar(getString(message.textRestId), length = length, action = action)
      }
      is MessageEvent.Error -> {
        host.showErrorSnackbar(getString(message.textRestId))
      }
    }
  }

  /**
   * Every dialog in the app wears [R.style.AlertDialog] - the tip ("?") popup's
   * surface, corners and entrance. Building them here instead of naming the
   * style at each call site keeps that one decision in one place.
   */
  protected fun dialog(
    @StyleRes style: Int = R.style.AlertDialog,
  ) = MaterialAlertDialogBuilder(requireContext(), style)

  /**
   * Pick one of a list, which is what most of these dialogs are for. Whether an
   * unchanged pick is worth acting on is left to the caller: for some of them
   * re-picking the current option is how the step gets redone.
   */
  protected fun <T> showSingleChoiceDialog(
    options: List<T>,
    selected: T?,
    label: (T) -> CharSequence,
    @StyleRes style: Int = R.style.AlertDialog,
    onPicked: (T) -> Unit,
  ) {
    dialog(style)
      .setSingleChoiceItems(options.map(label).toTypedArray(), options.indexOf(selected)) { picker, index ->
        onPicked(options[index])
        picker.dismiss()
      }.show()
  }

  protected open fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      isEnabled = false
      findNavControl()?.popBackStack()
    }
  }

  protected fun navigateTo(
    @IdRes destination: Int,
    bundle: Bundle? = null,
  ) {
    findNavControl()?.navigate(destination, bundle)
  }

  override fun isTipShown(tip: Tip) = (requireActivity() as TipsHost).isTipShown(tip)

  override fun showTip(tip: Tip) = (requireActivity() as TipsHost).showTip(tip)

  override fun setTipShow(tip: Tip) = (requireActivity() as TipsHost).showTip(tip)

  private fun clearAnimations() {
    animations.forEach { it?.cancel() }
    animators.forEach { it?.cancel() }
    animations.clear()
    animators.clear()
  }

  override fun onDestroyView() {
    snackbars.forEach { it?.dismiss() }
    clearAnimations()
    super.onDestroyView()
  }

  fun Fragment.requireAppContext(): Context = requireContext().applicationContext
}
