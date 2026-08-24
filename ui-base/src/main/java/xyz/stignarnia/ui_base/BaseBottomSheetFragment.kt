package xyz.stignarnia.ui_base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import xyz.stignarnia.ui_base.utilities.NavigationHost

abstract class BaseBottomSheetFragment(
  @param:LayoutRes val layoutResId: Int,
) : BottomSheetDialogFragment() {

  /**
   * Inflated against the Activity itself rather than against @style/AppTheme.
   *
   * The Activity's theme is AppTheme with the selected overlays already composed onto it - Material You, AMOLED - and those live on its Theme object, not in the style resource.
   * Wrapping it in the bare style put them back: ContextThemeWrapper copies the base theme and then applies the given resource with force, so AppTheme's own colours overwrote exactly the attributes the overlays were there to replace, and every sheet came up in plain light or dark.
   */
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = inflater.cloneInContext(requireActivity()).inflate(layoutResId, container, false)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    expandSheet()
  }

  protected fun navigateTo(
    @IdRes destination: Int,
    bundle: Bundle? = null,
  ) = (requireActivity() as NavigationHost).findNavControl()?.navigate(destination, bundle)

  protected fun isSheetExpanded(): Boolean {
    val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
    return behavior.state == BottomSheetBehavior.STATE_EXPANDED
  }

  protected fun expandSheet() {
    val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
    behavior.skipCollapsed = true
  }

  protected fun closeSheet() = (requireActivity() as NavigationHost).findNavControl()?.navigateUp()
}
