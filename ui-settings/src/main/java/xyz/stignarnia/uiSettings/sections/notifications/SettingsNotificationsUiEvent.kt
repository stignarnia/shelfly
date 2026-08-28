
package xyz.stignarnia.uiSettings.sections.notifications

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class SettingsNotificationsUiEvent<T>(
  action: T,
) : Event<T>(action) {
  /**
   * The system will not let this app post notifications.
   *
   * Deliberately not named after the remedy: whether that is a permission to request or a settings screen to open depends on the API level and on what has already been refused, and only the Fragment can tell.
   */
  data object NotificationsBlocked : SettingsNotificationsUiEvent<Unit>(Unit)
}
