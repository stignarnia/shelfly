package xyz.stignarnia.uiBase.utilities

import xyz.stignarnia.uiModel.Tip

interface TipsHost {
  fun isTipShown(tip: Tip): Boolean

  fun showTip(tip: Tip)

  fun setTipShow(tip: Tip)
}
