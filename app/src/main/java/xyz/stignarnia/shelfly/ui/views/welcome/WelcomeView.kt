package xyz.stignarnia.shelfly.ui.views.welcome

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import xyz.stignarnia.shelfly.R
import xyz.stignarnia.shelfly.databinding.ViewWelcomeBinding
import xyz.stignarnia.shelfly.databinding.ViewWelcomeStepApiKeyBinding
import xyz.stignarnia.shelfly.databinding.ViewWelcomeStepDisclaimerBinding
import xyz.stignarnia.shelfly.databinding.ViewWelcomeStepLanguageBinding
import xyz.stignarnia.shelfly.databinding.ViewWelcomeStepMessageBinding
import xyz.stignarnia.shelfly.databinding.ViewWelcomeStepWhatsNewBinding
import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeState
import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeStep
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.gone
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visible
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_settings.helpers.AppLanguage
import java.util.Locale

/**
 * Single host for every welcome screen.
 *
 * The card, the step indicator, the button row and the back affordance are shared;
 * only the body is swapped per step. Steps carry no presentation logic of their
 * own, so a new one costs a layout and a branch in [bindContent].
 *
 * Every piece of text is resolved through [strings] rather than from the layouts,
 * because the flow has to be readable in the app's own language before Android
 * has necessarily applied it. See [WelcomeState.displayLanguage].
 */
class WelcomeView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewWelcomeBinding.inflate(LayoutInflater.from(context), this)

  var onPrimaryClick: (() -> Unit)? = null
  var onSecondaryClick: (() -> Unit)? = null
  var onBackClick: (() -> Unit)? = null
  var onApiKeyChanged: ((String) -> Unit)? = null

  private var renderedStep: WelcomeStep? = null
  private var renderedLanguage: AppLanguage? = null
  private var apiKeyBinding: ViewWelcomeStepApiKeyBinding? = null
  private lateinit var strings: Resources

  init {
    layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    with(binding) {
      viewWelcomePrimaryButton.onClick { onPrimaryClick?.invoke() }
      viewWelcomeSecondaryButton.onClick { onSecondaryClick?.invoke() }
      viewWelcomeBackButton.onClick { onBackClick?.invoke() }
      // Full screen and edge to edge, so the bars are this view's problem. The
      // keyboard counts too: without it the buttons sit under the IME on the
      // key steps.
      viewWelcomeRoot.doOnApplyWindowInsets { view, insets, padding, _ ->
        val spacing = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        view.updatePadding(
          top = padding.top + spacing.top,
          bottom = padding.bottom + spacing.bottom,
        )
      }
    }
  }

  fun render(state: WelcomeState) {
    val languageChanged = renderedLanguage != state.displayLanguage
    if (languageChanged) {
      strings = resourcesFor(state.displayLanguage)
      renderedLanguage = state.displayLanguage
    }

    // Only reinflate on an actual step or language change, so typing does not
    // cost the body view its focus and the keyboard.
    if (languageChanged || renderedStep?.let { it::class } != state.step::class) {
      bindContent(state.step)
      renderedStep = state.step
    }

    with(binding) {
      viewWelcomeStepIndicator.visibleIf(state.total > 1)
      viewWelcomeStepIndicator.text = strings.getString(
        R.string.textWelcomeStepIndicator,
        state.index + 1,
        state.total,
      )
      viewWelcomeBackButton.text = strings.getText(R.string.textBack)
      viewWelcomeBackButton.visibleIf(state.isBackEnabled)
      viewWelcomePrimaryButton.text = strings.getText(state.step.primaryButton)
      viewWelcomePrimaryButton.isEnabled = state.isPrimaryEnabled
      renderSecondaryButton(state.step)
    }

    apiKeyBinding?.viewWelcomeStepApiKeyInput?.let {
      if (it.text.toString() != state.apiKeyDraft) {
        it.setText(state.apiKeyDraft)
        it.setSelection(state.apiKeyDraft.length)
      }
    }
  }

  private fun renderSecondaryButton(step: WelcomeStep) {
    val secondaryLabel = step.secondaryButton
    with(binding.viewWelcomeSecondaryButton) {
      when {
        // Declining means keeping the language already in use, which is not
        // always English, so this label cannot be a fixed resource.
        step is WelcomeStep.Language -> {
          text = strings.getString(R.string.textKeepLanguage, step.current.displayNameRaw)
          visible()
        }
        secondaryLabel != null -> {
          text = strings.getText(secondaryLabel)
          visible()
        }
        else -> {
          gone()
        }
      }
    }
  }

  @SuppressLint("SetTextI18n")
  private fun bindContent(step: WelcomeStep) {
    val inflater = LayoutInflater.from(context)
    val container = binding.viewWelcomeContent
    container.removeAllViews()
    apiKeyBinding = null
    binding.viewWelcomeContentScroll.scrollTo(0, 0)

    when (step) {
      is WelcomeStep.Language -> {
        ViewWelcomeStepLanguageBinding.inflate(inflater, container, true).apply {
          // Always English: it asks the question before the answer exists.
          viewWelcomeStepLanguageMessage.text =
            "It seems like your device's language is ${step.suggested.displayNameRaw}.\n" +
            "Would you like to use it in Shelfly?"
          viewWelcomeStepLanguageHint.text = strings.getText(R.string.textLanguagesChoose2)
        }
      }

      is WelcomeStep.Disclaimer -> {
        ViewWelcomeStepDisclaimerBinding.inflate(inflater, container, true).apply {
          viewWelcomeStepDisclaimerTitle.text = strings.getText(R.string.textDisclaimerTitle)
          viewWelcomeStepDisclaimerMessage.text = strings.getText(R.string.textDisclaimerText)
        }
      }

      is WelcomeStep.ApiKey -> {
        apiKeyBinding = ViewWelcomeStepApiKeyBinding.inflate(inflater, container, true).apply {
          viewWelcomeStepApiKeyTitle.text = strings.getText(step.title)
          viewWelcomeStepApiKeyMessage.text = strings.getText(step.message)
          viewWelcomeStepApiKeyInput.hint = strings.getText(step.hint)
          viewWelcomeStepApiKeyInput.doAfterTextChanged {
            onApiKeyChanged?.invoke(it?.toString().orEmpty())
          }
        }
      }

      is WelcomeStep.Notifications -> {
        bindMessageStep(
          inflater = inflater,
          icon = R.drawable.ic_notification_bell,
          title = R.string.textOnboardingNotificationsTitle,
          message = R.string.textOnboardingNotificationsMessage,
        )
      }

      is WelcomeStep.WebDavSync -> {
        bindMessageStep(
          inflater = inflater,
          icon = R.drawable.ic_cloud_upload,
          title = R.string.textOnboardingSyncTitle,
          message = R.string.textOnboardingSyncMessage,
        )
      }

      is WelcomeStep.WhatsNew -> {
        ViewWelcomeStepWhatsNewBinding.inflate(inflater, container, true).apply {
          viewWelcomeStepWhatsNewTitle.text = strings.getText(R.string.textWhatsNewTitle)
          viewWelcomeStepWhatsNewSubtitle.text = strings.getString(R.string.textWhatsNewSubtitle, step.version)
          viewWelcomeStepWhatsNewMessage.text = step.notes
        }
      }
    }
  }

  private fun bindMessageStep(
    inflater: LayoutInflater,
    icon: Int,
    title: Int,
    message: Int,
  ) {
    ViewWelcomeStepMessageBinding.inflate(inflater, binding.viewWelcomeContent, true).apply {
      viewWelcomeStepMessageIcon.setImageResource(icon)
      viewWelcomeStepMessageTitle.text = strings.getText(title)
      viewWelcomeStepMessageText.text = strings.getText(message)
    }
  }

  /**
   * Resources bound to one language, used for every string this view shows. The
   * theme is left alone - only the locale is overridden - so colours and
   * dimensions still come from the host.
   */
  private fun resourcesFor(language: AppLanguage): Resources {
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(Locale(language.code))
    return context.createConfigurationContext(configuration).resources
  }
}
