package xyz.stignarnia.ui_model

/**
 * What a single widget is themed as, stored per widget id.
 *
 * The entries after [FOLLOW_APP] are the app's own themes, and their names are deliberately the ids AppTheme persists - see xyz.stignarnia.ui_settings.helpers.AppTheme - so a widget's choice and the app's choice can be compared and mapped as strings.
 * That indirection is what lets this enum live down here, where neither the repository nor the widgets can see the UI layer's AppTheme.
 *
 * [FOLLOW_APP] is the default and the only value a widget has until its configuration screen is opened, so a widget that was never configured moves with the app's theme.
 * These names are storage and must not be renamed casually.
 */
enum class WidgetTheme {
  FOLLOW_APP,
  SYSTEM,
  LIGHT,
  DARK,
  DYNAMIC_SYSTEM,
  DYNAMIC_LIGHT,
  DYNAMIC_DARK,
  ;

  companion object {
    fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: FOLLOW_APP
  }
}

/**
 * The pure black switch for a single widget.
 *
 * A third state rather than a boolean, because "off" and "whatever the app says" are different answers: a widget left alone has to keep following the app's switch, and one turned off has to stay off when the app's is turned on.
 */
enum class WidgetAmoled {
  FOLLOW_APP,
  ON,
  OFF,
  ;

  companion object {
    fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: FOLLOW_APP
  }
}
