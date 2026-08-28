package xyz.stignarnia.shelfly.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.shelfly.R
import xyz.stignarnia.shelfly.databinding.ActivityWidgetConfigBinding
import xyz.stignarnia.shelfly.ui.ThemeApplier
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.common.views.modal.ModalBuilder
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiModel.WidgetAmoled
import xyz.stignarnia.uiModel.WidgetTheme
import xyz.stignarnia.uiSettings.helpers.AppTheme
import javax.inject.Inject

/**
 * The screen behind a widget's own settings in the launcher.
 *
 * Reached from the launcher rather than from the app, because the choice belongs to one widget and not to the collection: two copies of the same widget can be themed apart from each other, and neither is a property of the app.
 * Named as the configure Activity only in res/xml-v31 - see the widget providers - so it exists exactly where the launcher offers a settings button for an already placed widget, and where a widget can be themed at all.
 *
 * The theme list comes from AppTheme.supported(), so a device without Material You is offered the themes it can honour and no others.
 *
 * Every pick is written and pushed to the widget as it is made, the way the app's own Settings behave.
 * The button at the bottom therefore only closes the screen; it is there because a configuration Activity has to return a result, and because a screen with no way out reads as unfinished.
 */
@AndroidEntryPoint
class WidgetConfigActivity : AppCompatActivity() {
  @Inject lateinit var settingsRepository: SettingsRepository

  private lateinit var binding: ActivityWidgetConfigBinding
  private var widgetId = INVALID_APPWIDGET_ID

  override fun onCreate(savedInstanceState: Bundle?) {
    // Before super, exactly as in BaseActivity: the theme has to be settled before anything is inflated against it.
    ThemeApplier.applyNightMode(this)
    ThemeApplier.applyOverlays(this)
    super.onCreate(savedInstanceState)

    widgetId = intent?.extras?.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID) ?: INVALID_APPWIDGET_ID
    if (widgetId == INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    // A configuration Activity that is dismissed has cancelled, and the launcher drops a widget it was placing.
    setResult(Activity.RESULT_CANCELED, resultIntent())

    binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.widgetConfigDoneButton.onClick {
      setResult(Activity.RESULT_OK, resultIntent())
      finish()
    }

    with(binding.widgetConfigTransparencySlider) {
      addOnChangeListener { _, value, fromUser ->
        if (fromUser) {
          binding.widgetConfigTransparencyValue.text = getString(R.string.textPercentage, value.toInt())
        }
      }
      addOnSliderTouchListener(
        object : Slider.OnSliderTouchListener {
          override fun onStartTrackingTouch(slider: Slider) = Unit

          override fun onStopTrackingTouch(slider: Slider) {
            val transparency = slider.value.toInt()
            settingsRepository.widgets.setWidgetTransparency(widgetId, transparency)
            onChanged()
          }
        },
      )
    }

    render()
  }

  private fun render() {
    val theme = settingsRepository.widgets.getWidgetTheme(widgetId)
    val amoled = settingsRepository.widgets.getWidgetAmoled(widgetId)
    val transparency = settingsRepository.widgets.getWidgetTransparency(widgetId)

    // A widget on the app's theme is on the app's switch as well, so the switch is the app's to set and shows what the app says.
    val followsApp = theme == WidgetTheme.FOLLOW_APP
    val canBeDark = theme.appTheme().canBeDark
    val canToggle = canBeDark && !followsApp

    with(binding) {
      widgetConfigThemeValue.text = theme.label()
      widgetConfigTheme.onClick { showThemeModal(theme) }

      // The switch keeps showing its value while disabled, so a trip through a light theme and back leaves the choice intact - as in Settings.
      widgetConfigAmoled.isEnabled = canToggle
      widgetConfigAmoledSwitch.isEnabled = canToggle
      widgetConfigAmoled.alpha = if (canToggle) 1F else DISABLED_ALPHA
      widgetConfigAmoledSwitch.isChecked = if (followsApp) settingsRepository.isAmoled else amoled.isOn()
      widgetConfigAmoled.onClick {
        if (!canToggle) return@onClick
        // Touching the switch pins it: from here the widget keeps this answer whatever the app's own switch is set to.
        settingsRepository.widgets.setWidgetAmoled(widgetId, if (amoled.isOn()) WidgetAmoled.OFF else WidgetAmoled.ON)
        onChanged()
      }

      widgetConfigTransparencyValue.text = getString(R.string.textPercentage, transparency)
      if (widgetConfigTransparencySlider.value.toInt() != transparency) {
        widgetConfigTransparencySlider.value = transparency.toFloat()
      }
    }
  }

  private fun showThemeModal(current: WidgetTheme) {
    val options = listOf(WidgetTheme.FOLLOW_APP) + AppTheme.supported().map { WidgetTheme.fromName(it.id) }
    ModalBuilder(window.decorView)
      .setTitle(R.string.textSettingsThemeTitle)
      .setSingleChoiceItems(options.map { it.label() }, options.indexOf(current)) { index ->
        val picked = options[index]
        if (picked == current) return@setSingleChoiceItems
        settingsRepository.widgets.setWidgetTheme(widgetId, picked)
        // Going back to the app's theme hands the pure black switch back too, so a value pinned under some earlier theme is not left behind to be honoured again later.
        if (picked == WidgetTheme.FOLLOW_APP) {
          settingsRepository.widgets.setWidgetAmoled(widgetId, WidgetAmoled.FOLLOW_APP)
        }
        onChanged()
      }.show()
  }

  private fun onChanged() {
    (applicationContext as WidgetsProvider).requestAllWidgetsUpdate()
    render()
  }

  private fun resultIntent() = Intent().putExtra(EXTRA_APPWIDGET_ID, widgetId)

  /** What a widget theme is called on screen: the app's own name for it, or "same as app" for the one that is not a theme. */
  private fun WidgetTheme.label(): CharSequence =
    if (this == WidgetTheme.FOLLOW_APP) {
      getString(R.string.textWidgetConfigThemeApp)
    } else {
      getString(appTheme().displayName)
    }

  /** The app theme this widget resolves to, which for FOLLOW_APP is whatever the app is set to right now. */
  private fun WidgetTheme.appTheme(): AppTheme =
    if (this == WidgetTheme.FOLLOW_APP) {
      AppTheme.fromId(settingsRepository.themeId)
    } else {
      AppTheme.fromId(name)
    }

  private fun WidgetAmoled.isOn() =
    when (this) {
      WidgetAmoled.ON -> true
      WidgetAmoled.OFF -> false
      WidgetAmoled.FOLLOW_APP -> settingsRepository.isAmoled
    }

  private companion object {
    const val DISABLED_ALPHA = 0.5F
  }
}
