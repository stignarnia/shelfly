package xyz.stignarnia.shelfly.ui.main.cases

import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.uiModel.Tip
import javax.inject.Inject
import javax.inject.Named

@ViewModelScoped
class MainTipsCase
  @Inject
  constructor(
    @param:Named("tipsPreferences") private val sharedPreferences: SharedPreferences,
  ) {
    fun isTipShown(tip: Tip) = sharedPreferences.getBoolean(tip.name, false)

    fun setTipShown(tip: Tip) {
      sharedPreferences.edit { putBoolean(tip.name, true) }
    }
  }
