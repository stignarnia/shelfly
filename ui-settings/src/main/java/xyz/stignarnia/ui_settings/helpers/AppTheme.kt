package xyz.stignarnia.ui_settings.helpers

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import xyz.stignarnia.ui_settings.R

enum class AppTheme(
  val code: Int,
  @StringRes val displayName: Int,
) {
  DARK(MODE_NIGHT_YES, R.string.textThemeDark),
  ;

  companion object {
    fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: DARK
  }
}
