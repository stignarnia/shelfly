@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_settings.sections.notifications

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class SettingsNotificationsUiEvent<T>(
  action: T,
) : Event<T>(action) {
  data object RequestNotificationsPermission : SettingsNotificationsUiEvent<Unit>(Unit)
}
