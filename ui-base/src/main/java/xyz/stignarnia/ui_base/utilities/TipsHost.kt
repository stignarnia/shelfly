package xyz.stignarnia.ui_base.utilities

import xyz.stignarnia.ui_model.Tip

interface TipsHost {
  fun isTipShown(tip: Tip): Boolean

  fun showTip(tip: Tip)

  fun setTipShow(tip: Tip)
}
