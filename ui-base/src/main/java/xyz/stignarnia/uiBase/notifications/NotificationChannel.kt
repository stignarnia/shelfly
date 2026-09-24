package xyz.stignarnia.uiBase.notifications

import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import xyz.stignarnia.uiBase.R

/**
 * The name and description are what Android shows in the app's notification settings.
 * The id is the enum's name, which Android keeps across launches, so renaming an entry orphans the channel the user has already configured.
 */
enum class NotificationChannel(
  @field:StringRes val displayName: Int,
  @field:StringRes val description: Int,
  val importance: Int,
) {
  GENERAL_INFO(
    R.string.textNotificationChannelGeneral,
    R.string.textNotificationChannelGeneralDescription,
    NotificationManagerCompat.IMPORTANCE_HIGH,
  ),
  SHOWS_INFO(
    R.string.textNotificationChannelShows,
    R.string.textNotificationChannelShowsDescription,
    NotificationManagerCompat.IMPORTANCE_DEFAULT,
  ),
  EPISODES_ANNOUNCEMENTS(
    R.string.textNotificationChannelEpisodes,
    R.string.textNotificationChannelEpisodesDescription,
    NotificationManagerCompat.IMPORTANCE_DEFAULT,
  ),
  MOVIES_ANNOUNCEMENTS(
    R.string.textNotificationChannelMovies,
    R.string.textNotificationChannelMoviesDescription,
    NotificationManagerCompat.IMPORTANCE_DEFAULT,
  ),
  SYNC(
    R.string.textNotificationChannelSync,
    R.string.textNotificationChannelSyncDescription,
    NotificationManagerCompat.IMPORTANCE_LOW,
  ),
}
