package xyz.stignarnia.shelfly.ui.main.cases

import android.content.SharedPreferences
import xyz.stignarnia.common.Config
import xyz.stignarnia.shelfly.BuildConfig
import xyz.stignarnia.ui_model.Tip
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import javax.inject.Named

@ViewModelScoped
class MainTipsCase @Inject constructor(
  @Named("tipsPreferences") private val sharedPreferences: SharedPreferences,
) {

  fun isTipShown(tip: Tip) =
    when {
      BuildConfig.DEBUG -> !Config.SHOW_TIPS || sharedPreferences.getBoolean(tip.name, false)
      else -> sharedPreferences.getBoolean(tip.name, false)
    }

  fun setTipShown(tip: Tip) {
    sharedPreferences.edit().putBoolean(tip.name, true).apply()
  }
}
