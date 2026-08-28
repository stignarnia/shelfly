package xyz.stignarnia.uiBase.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.common.behaviour.SearchViewBehaviour
import xyz.stignarnia.uiBase.databinding.ViewSearchBinding
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.expandTouch
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf

class SearchView :
  FrameLayout,
  CoordinatorLayout.AttachedBehavior {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  val binding = ViewSearchBinding.inflate(LayoutInflater.from(context), this, true)

  var onSettingsClickListener: (() -> Unit)? = null
  var onStatsClickListener: (() -> Unit)? = null

  init {
    with(binding) {
      searchSettingsIcon.expandTouch()
      searchSettingsIcon.onClick { onSettingsClickListener?.invoke() }
      searchStatsIcon.onClick { onStatsClickListener?.invoke() }
    }
  }

  var hint: String
    get() = binding.searchViewInput.hint.toString()
    set(value) {
      with(binding) {
        searchViewInput.hint = value
        searchViewText.text = value
      }
    }

  var settingsIconVisible
    get() = binding.searchSettingsIcon.isVisible
    set(value) {
      binding.searchSettingsIcon.visibleIf(value)
    }

  var statsIconVisible
    get() = binding.searchStatsIcon.isVisible
    set(value) {
      binding.searchStatsIcon.visibleIf(value)
    }

  var isSearching = false

  override fun onAttachedToWindow() {
    doOnApplyWindowInsets { _, insets, _, _ ->
      val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
      applyWindowInsetBehaviour(context.dimenToPx(R.dimen.spaceNormal) + inset)
    }
    super.onAttachedToWindow()
  }

  fun applyWindowInsetBehaviour(newInset: Int) {
    updateLayoutParams {
      (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = SearchViewBehaviour(newInset)
    }
  }

  override fun getBehavior() = SearchViewBehaviour(context.dimenToPx(R.dimen.spaceNormal))

  override fun setEnabled(enabled: Boolean) {
    binding.searchViewInput.isEnabled = enabled
    super.setEnabled(enabled)
  }
}
