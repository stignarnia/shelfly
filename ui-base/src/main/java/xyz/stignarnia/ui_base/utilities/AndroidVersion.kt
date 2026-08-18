package xyz.stignarnia.ui_base.utilities

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * The platform versions this app branches on, in one place.
 *
 * minSdk is 23, so anything newer than that has to be asked for rather than assumed.
 * Every check goes through here so the set of versions the app cares about is a list you can read rather than something you find by grepping for SDK_INT.
 *
 * [ChecksSdkIntAtLeast] is what makes these worth having over a raw comparison: it tells lint that a body guarded by one of these properties is only reached above that version, so calls to newer APIs inside are checked as usual instead of being flagged.
 * That is also why none of the call sites need @TargetApi - a suppression that hides the mistake rather than proving it cannot happen.
 * @RequiresApi still has a place, but on declarations that genuinely require a version and push that requirement onto their callers.
 */
object AndroidVersion {

  /** API 26. Notification channels, adaptive icons, the timeout overload of requestNetwork. */
  @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
  val isAtLeastAndroid8: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

  /** API 31. StrictMode's unsafe intent launch detection. */
  @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
  val isAtLeastAndroid12: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

  /** API 33. The POST_NOTIFICATIONS permission, per-app language, the typed Bundle getters. */
  @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
  val isAtLeastAndroid13: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

  /** API 34. The dynamic colour surface roles Material You is drawn from. */
  @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  val isAtLeastAndroid14: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}
