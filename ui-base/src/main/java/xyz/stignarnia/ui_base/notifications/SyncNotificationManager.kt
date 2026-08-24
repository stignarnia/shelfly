package xyz.stignarnia.ui_base.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.ui_base.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncNotificationManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {

  companion object {
    const val SYNC_PROGRESS_NOTIFICATION_ID = 916
    const val SYNC_ERROR_NOTIFICATION_ID = 917
    const val SYNC_SUCCESS_NOTIFICATION_ID = 918

    const val EXTRA_OPEN_WEBDAV_SETTINGS = "EXTRA_OPEN_WEBDAV_SETTINGS"
  }

  private val notificationManager by lazy { NotificationManagerCompat.from(context) }

  /**
   * Deliberately not [NotificationCompat.Builder.setOngoing].
   * This notification is not backed by a foreground service, so nothing guarantees the code that cancels it ever runs - a worker killed for memory, or by the platform's execution limit, takes its process down mid sync.
   * An ongoing notification cannot be swiped away, so that would leave a permanent "Syncing…" the user has no way to be rid of.
   * Dismissible plus [cancelStaleProgress] on the next launch is worth more than a notification the user cannot accidentally clear.
   */
  @SuppressLint("MissingPermission")
  fun showProgress(
    title: String,
    message: String,
  ) {
    val notification = NotificationCompat
      .Builder(context, NotificationChannel.SYNC.name)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(message)
      .setProgress(0, 0, true)
      .setAutoCancel(false)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setColor(ContextCompat.getColor(context, R.color.colorNotificationDark))
      .build()

    notificationManager.notify(SYNC_PROGRESS_NOTIFICATION_ID, notification)
  }

  @SuppressLint("MissingPermission")
  fun showSuccess(
    title: String,
    message: String,
  ) {
    val notification = NotificationCompat
      .Builder(context, NotificationChannel.SYNC.name)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(message)
      .setAutoCancel(true)
      .setTimeoutAfter(3000)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setColor(ContextCompat.getColor(context, R.color.colorNotificationDark))
      .build()

    notificationManager.notify(SYNC_SUCCESS_NOTIFICATION_ID, notification)
  }

  @SuppressLint("MissingPermission")
  fun showError(
    title: String,
    message: String,
  ) {
    val notification = NotificationCompat
      .Builder(context, NotificationChannel.SYNC.name)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setContentIntent(createSettingsIntent())
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setColor(ContextCompat.getColor(context, R.color.colorNotificationDark))
      .build()

    notificationManager.notify(SYNC_ERROR_NOTIFICATION_ID, notification)
  }

  fun cancelProgress() {
    notificationManager.cancel(SYNC_PROGRESS_NOTIFICATION_ID)
  }

  /**
   * Clears a progress notification left behind by a sync whose process died before it could finish.
   * Called once per process start, where no sync can be running yet, so anything still showing is by definition stale.
   */
  fun cancelStaleProgress() = cancelProgress()

  fun cancelError() {
    notificationManager.cancel(SYNC_ERROR_NOTIFICATION_ID)
  }

  private fun createSettingsIntent(): PendingIntent {
    val targetClass = Class.forName(Config.HOST_ACTIVITY_NAME)
    val notifyIntent = Intent(context, targetClass).apply {
      putExtra(EXTRA_OPEN_WEBDAV_SETTINGS, true)
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
      context,
      SYNC_ERROR_NOTIFICATION_ID,
      notifyIntent,
      FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
    )
  }
}
