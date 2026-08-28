package xyz.stignarnia.uiBase.notifications

import androidx.core.app.NotificationManagerCompat

enum class NotificationChannel(
  val displayName: String,
  val description: String,
  val importance: Int,
) {
  GENERAL_INFO(
    "General Info",
    "General information and announcements",
    NotificationManagerCompat.IMPORTANCE_HIGH,
  ),
  SHOWS_INFO(
    "Shows Info",
    "Shows related information",
    NotificationManagerCompat.IMPORTANCE_DEFAULT,
  ),
  EPISODES_ANNOUNCEMENTS(
    "Episodes Announcements",
    "Episodes and seasons announcements",
    NotificationManagerCompat.IMPORTANCE_DEFAULT,
  ),
  MOVIES_ANNOUNCEMENTS(
    "Movies Announcements",
    "Movies announcements",
    NotificationManagerCompat.IMPORTANCE_DEFAULT,
  ),
  SYNC(
    "Sync & Backup",
    "Sync and backup notifications",
    NotificationManagerCompat.IMPORTANCE_LOW,
  ),
}
