package xyz.stignarnia.shelfly.ui.main.delegates

import androidx.lifecycle.DefaultLifecycleObserver
import xyz.stignarnia.shelfly.databinding.ActivityMainBinding
import xyz.stignarnia.shelfly.ui.main.MainViewModel
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.common.views.modal.ModalBuilder
import xyz.stignarnia.uiBase.utilities.TipsHost
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiModel.Tip

interface TipsDelegate : TipsHost {
  fun registerTipsDelegate(
    viewModel: MainViewModel,
    binding: ActivityMainBinding,
  )

  fun showAllTips()

  fun hideAllTips()
}

class MainTipsDelegate :
  TipsDelegate,
  DefaultLifecycleObserver {
  private lateinit var viewModel: MainViewModel
  private lateinit var binding: ActivityMainBinding

  private val tips by lazy {
    mapOf(
      Tip.MENU_DISCOVER to binding.tutorialTipDiscover,
      Tip.MENU_MY_SHOWS to binding.tutorialTipMyShows,
      Tip.MENU_MODES to binding.tutorialTipModeMenu,
    )
  }

  override fun registerTipsDelegate(
    viewModel: MainViewModel,
    binding: ActivityMainBinding,
  ) {
    this.viewModel = viewModel
    this.binding = binding
    setupTips()
  }

  private fun setupTips() {
    tips.entries.forEach { (tip, view) ->
      view.visibleIf(!isTipShown(tip))
      view.onClick {
        it.gone()
        showTip(tip)
      }
    }
  }

  override fun setTipShow(tip: Tip) = viewModel.setTipShown(tip)

  override fun isTipShown(tip: Tip) = viewModel.isTipShown(tip)

  override fun showTip(tip: Tip) {
    ModalBuilder(binding.root)
      .setTitle(R.string.textTip)
      .setMessage(tip.textResId)
      .setPositiveButton(R.string.textOk) { it.dismiss() }
      .show()
    setTipShow(tip)
  }

  override fun showAllTips() {
    tips.entries.forEach { (tip, view) -> view.visibleIf(!isTipShown(tip)) }
  }

  override fun hideAllTips() {
    tips.values.forEach { it.gone() }
  }
}
