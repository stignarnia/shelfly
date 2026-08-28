package xyz.stignarnia.uiPeople.details.recycler.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.uiBase.utilities.extensions.copyToClipboard
import xyz.stignarnia.uiBase.utilities.extensions.onLongClick
import xyz.stignarnia.uiBase.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.ViewPersonDetailsBioBinding
import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsItem

class PersonDetailsBioView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewPersonDetailsBioBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    with(binding) {
      personBioText.setInitialLines(5)
      personBioText.onLongClick {
        context.copyToClipboard(personBioText.text.toString())
        snackbarLayout.showInfoSnackbar(context.getString(R.string.textCopiedToClipboard), length = 1250)
      }
    }
  }

  fun bind(item: PersonDetailsItem.MainBio) {
    with(binding) {
      when {
        item.biography.isNullOrBlank() -> personBioText.text = context.getString(R.string.textNoDescription)
        !item.biographyTranslation.isNullOrBlank() -> personBioText.text = item.biographyTranslation
        else -> personBioText.text = item.biography
      }
    }
  }
}
