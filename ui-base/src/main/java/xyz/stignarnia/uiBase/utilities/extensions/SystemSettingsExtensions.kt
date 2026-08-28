package xyz.stignarnia.uiBase.utilities.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.Fragment
import xyz.stignarnia.uiBase.utilities.AndroidVersion

/**
 * Opens the system screen where the user can turn this app's notifications back on.
 *
 * There is no single intent for it across the versions the app supports, so this tries the most specific one first and falls back.
 * ACTION_APP_NOTIFICATION_SETTINGS lands directly on the notification page but only exists from API 26; below that the app details page is the closest thing, and it has been there since long before minSdk.
 *
 * @return false when neither resolved, so the caller can say something rather than appear to do nothing.
 */
fun Context.openNotificationSettings(): Boolean {
  val intents =
    buildList {
      if (AndroidVersion.isAtLeastAndroid8) {
        add(
          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
      }
      add(
        Intent(
          Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
          Uri.fromParts("package", packageName, null),
        ),
      )
    }

  return intents.any { intent ->
    try {
      startActivity(intent)
      true
    } catch (error: ActivityNotFoundException) {
      false
    }
  }
}

fun Fragment.openNotificationSettings() = requireContext().openNotificationSettings()
