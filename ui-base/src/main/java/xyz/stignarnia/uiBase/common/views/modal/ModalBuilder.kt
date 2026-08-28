package xyz.stignarnia.uiBase.common.views.modal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visible

/**
 * Assembles a [ModalView].
 * The call shape follows the alert dialog builder it replaced, so a screen reads the same as before, with one deliberate difference: **a button never closes the modal by itself**.
 * The confirm lambda is handed the [ModalView] and dismisses when it is satisfied, which is what lets the backup retention field reject a bad number without throwing away what was typed.
 *
 * [anchor] only has to be a view in the host's hierarchy; the modal is added to that host's content root.
 */
class ModalBuilder(
  private val anchor: View,
) {
  private val modal = ModalView(anchor.context)
  private val binding = modal.binding
  private val inflater = LayoutInflater.from(anchor.context)

  fun setTitle(
    @StringRes textResId: Int,
  ) = apply { binding.modalTitle.show(anchor.context.getString(textResId)) }

  fun setMessage(
    @StringRes textResId: Int,
  ) = apply { binding.modalMessage.show(anchor.context.getString(textResId)) }

  fun setMessage(text: CharSequence) = apply { binding.modalMessage.show(text) }

  fun setView(view: View) =
    apply {
      binding.modalContent.addView(view)
      binding.modalContentScroll.visible()
    }

  /** A plain list of choices, with nothing marked as current. */
  fun setItems(
    items: List<CharSequence>,
    onPicked: (Int) -> Unit,
  ) = addChoices(items, checkedIndex = -1, onPicked = onPicked)

  /**
   * A list of choices with the current one ticked.
   * Picking closes the modal, since that is the whole interaction.
   *
   * [textSizeSp] exists for the date format picker, whose labels are long enough that they used to need their own dialog style.
   */
  fun setSingleChoiceItems(
    items: List<CharSequence>,
    checkedIndex: Int,
    textSizeSp: Float = DEFAULT_ITEM_TEXT_SIZE_SP,
    onPicked: (Int) -> Unit,
  ) = addChoices(items, checkedIndex, textSizeSp, onPicked)

  fun setPositiveButton(
    @StringRes textResId: Int,
    onClick: (ModalView) -> Unit,
  ) = apply {
    binding.modalPositiveButton.run {
      setText(textResId)
      visible()
      onClick { onClick(modal) }
    }
  }

  /**
   * A side action on its own centred row above the confirm, which leaves the modal open - the WebDAV form tests its credentials from here, and the result lands in the form above.
   */
  fun setNeutralButton(
    @StringRes textResId: Int,
    onClick: () -> Unit,
  ) = apply {
    binding.modalNeutralButton.run {
      setText(textResId)
      visible()
      onClick { onClick() }
    }
  }

  fun setNegativeButton(
    @StringRes textResId: Int,
    onClick: () -> Unit = {},
  ) = apply {
    binding.modalNegativeButton.run {
      setText(textResId)
      visible()
      onClick {
        onClick()
        modal.dismiss()
      }
    }
  }

  fun setOnDismiss(action: () -> Unit) = apply { modal.onDismissed = action }

  fun show(): ModalView {
    host().addView(modal)
    modal.show()
    return modal
  }

  private fun addChoices(
    items: List<CharSequence>,
    checkedIndex: Int,
    textSizeSp: Float = DEFAULT_ITEM_TEXT_SIZE_SP,
    onPicked: (Int) -> Unit,
  ) = apply {
    items.forEachIndexed { index, label ->
      val row = inflater.inflate(R.layout.view_modal_choice_item, binding.modalContent, false) as CheckedTextView
      row.text = label
      row.textSize = textSizeSp
      row.isChecked = index == checkedIndex
      row.onClick {
        onPicked(index)
        modal.dismiss()
      }
      binding.modalContent.addView(row)
    }
    binding.modalContentScroll.visible()
  }

  /**
   * The content view rather than the root, so the modal sits under the system bars' insets the way the rest of the screen does.
   */
  private fun host(): ViewGroup = anchor.rootView.findViewById(android.R.id.content) ?: anchor.rootView as ViewGroup

  private fun TextView.show(value: CharSequence) {
    text = value
    isVisible = true
  }

  companion object {
    const val DEFAULT_ITEM_TEXT_SIZE_SP = 16F
  }
}
