package xyz.stignarnia.uiSettings.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.children
import com.google.android.material.chip.Chip
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiSettings.databinding.ViewSettingsFiltersBinding
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter.API_KEYS
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter.BACKUP
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter.GENERAL
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter.MISC
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter.NOTIFICATIONS
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter.SPOILERS
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter.WIDGETS

class SettingsFiltersView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewSettingsFiltersBinding.inflate(LayoutInflater.from(context), this)

  var onFilterClick: ((SettingsFilter?) -> Unit)? = null
  private var selectedFilter: SettingsFilter? = null

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    initView()
  }

  private fun initView() {
    with(binding) {
      apiKeysChip.onClick(safe = false) {
        selectedFilter = if (selectedFilter == API_KEYS) null else API_KEYS
        onFilterClick?.invoke(selectedFilter)
      }
      generalChip.onClick(safe = false) {
        selectedFilter = if (selectedFilter == GENERAL) null else GENERAL
        onFilterClick?.invoke(selectedFilter)
      }
      notificationsChip.onClick(safe = false) {
        selectedFilter = if (selectedFilter == NOTIFICATIONS) null else NOTIFICATIONS
        onFilterClick?.invoke(selectedFilter)
      }
      spoilersChip.onClick(safe = false) {
        selectedFilter = if (selectedFilter == SPOILERS) null else SPOILERS
        onFilterClick?.invoke(selectedFilter)
      }
      widgetsChip.onClick(safe = false) {
        selectedFilter = if (selectedFilter == WIDGETS) null else WIDGETS
        onFilterClick?.invoke(selectedFilter)
      }
      backupChip.onClick(safe = false) {
        selectedFilter = if (selectedFilter == BACKUP) null else BACKUP
        onFilterClick?.invoke(selectedFilter)
      }
      miscChip.onClick(safe = false) {
        selectedFilter = if (selectedFilter == MISC) null else MISC
        onFilterClick?.invoke(selectedFilter)
      }
    }
  }

  fun clear() {
    selectedFilter = null
    binding.chipsGroup.children.forEach {
      (it as? Chip)?.isChecked = false
    }
  }

  override fun setEnabled(enabled: Boolean) {
    binding.chipsGroup.children.forEach { it.isEnabled = enabled }
  }

  enum class SettingsFilter {
    API_KEYS,
    GENERAL,
    NOTIFICATIONS,
    SPOILERS,
    WIDGETS,
    BACKUP,
    MISC,
  }
}
