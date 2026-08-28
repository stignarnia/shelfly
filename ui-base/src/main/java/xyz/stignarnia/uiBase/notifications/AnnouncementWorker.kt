package xyz.stignarnia.uiBase.notifications

import android.Manifest
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import xyz.stignarnia.common.Config
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.utilities.AndroidVersion
import kotlin.random.Random

class AnnouncementWorker(
  context: Context,
  workerParams: WorkerParameters,
) : Worker(context, workerParams) {
  companion object {
    const val DATA_SHOW_ID = "DATA_SHOW_ID"
    const val DATA_MOVIE_ID = "DATA_MOVIE_ID"
    const val DATA_TITLE = "DATA_TITLE"
    const val DATA_CONTENT = "DATA_CONTENT"
    const val DATA_CHANNEL = "DATA_CHANNEL"
    const val DATA_IMAGE_URL = "DATA_IMAGE_URL"
  }

  override fun doWork(): Result {
    val color = R.color.colorNotificationDark

    val title = inputData.getString(DATA_TITLE)

    val notification =
      NotificationCompat
        .Builder(applicationContext, inputData.getString(DATA_CHANNEL)!!)
        .setContentIntent(createIntent())
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(inputData.getString(DATA_CONTENT))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setColor(ContextCompat.getColor(applicationContext, color))

    val imageUrl = inputData.getString(DATA_IMAGE_URL)
    if ((imageUrl ?: "").isNotBlank()) {
      val target =
        Glide
          .with(applicationContext)
          .asBitmap()
          .load(imageUrl)
          .submit()
      try {
        val bitmap = target.get()
        notification.setLargeIcon(bitmap)
      } catch (e: Exception) {
        // NOOP
      } finally {
        Glide.with(applicationContext).clear(target)
      }
    }

    val notificationId =
      when {
        !title.isNullOrBlank() -> title.hashCode()
        else -> Random.nextInt()
      }

    // POST_NOTIFICATIONS became a runtime permission in Android 13; below that it does not exist and posting is always allowed.
    // Checked here rather than through a helper, because notify() drops the notification silently when the permission was refused.
    if (AndroidVersion.isAtLeastAndroid13 &&
      ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      // Nothing to retry: a refused permission will not appear on a later attempt.
      return Result.success()
    }

    NotificationManagerCompat
      .from(applicationContext)
      .notify(notificationId, notification.build())

    return Result.success()
  }

  private fun createIntent(): PendingIntent {
    var requestCode = 0L
    val targetClass = Class.forName(Config.HOST_ACTIVITY_NAME)
    val notifyIntent =
      Intent(applicationContext, targetClass).apply {
        val showId = inputData.getLong(DATA_SHOW_ID, -1)
        val movieId = inputData.getLong(DATA_MOVIE_ID, -1)
        when {
          showId != -1L -> {
            putExtra("EXTRA_SHOW_ID", showId.toString())
            requestCode = showId
          }

          movieId != -1L -> {
            putExtra("EXTRA_MOVIE_ID", movieId.toString())
            requestCode = movieId
          }
        }
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    return PendingIntent.getActivity(
      applicationContext,
      requestCode.toInt(),
      notifyIntent,
      FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
    )
  }
}
