package xyz.stignarnia.uiPeople.details.recycler.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import xyz.stignarnia.common.Config
import xyz.stignarnia.uiBase.utilities.extensions.capitalizeWords
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.extensions.withFailListener
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.ViewPersonDetailsInfoBinding
import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsItem

class PersonDetailsInfoView : ConstraintLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewPersonDetailsInfoBinding.inflate(LayoutInflater.from(context), this)

  private val topLeftCornerRadius by lazy { context.dimenToPx(R.dimen.personImageCorner).toFloat() }
  private val cornerRadius by lazy { context.dimenToPx(R.dimen.mediaTileCorner).toFloat() }
  private val spaceNormal by lazy { context.dimenToPx(R.dimen.spaceNormal) }

  var onLinksClickListener: ((Person) -> Unit)? = null
  var onImageClickListener: (() -> Unit)? = null

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    updatePadding(left = spaceNormal, right = spaceNormal)
    clipToPadding = false
  }

  fun bind(item: PersonDetailsItem.MainInfo) {
    with(binding) {
      viewPersonDetailsTitle.text = item.person.name
      viewPersonDetailsSubtitle.text = item.person.characters.joinToString(", ")
      viewPersonDetailsLinkIcon.onClick { onLinksClickListener?.invoke(item.person) }
      viewPersonDetailsImage.onClick { onImageClickListener?.invoke() }
      viewPersonDetailsPlaceholder.onClick { onImageClickListener?.invoke() }

      item.person.birthday?.let { date ->
        viewPersonDetailsBirthdayLabel.visible()
        viewPersonDetailsBirthdayValue.visible()
        viewPersonDetailsAgeLabel.visible()
        viewPersonDetailsAgeValue.visible()
        val birthdayText =
          item.dateFormat
            ?.format(date)
            ?.capitalizeWords()
            ?.plus(if (!item.person.birthplace.isNullOrBlank()) "\n${item.person.birthplace}" else "")
        viewPersonDetailsBirthdayValue.text = birthdayText
        viewPersonDetailsAgeValue.text = item.person.getAge().toString()
      }
      item.person.deathday?.let { date ->
        viewPersonDetailsDeathdayLabel.visible()
        viewPersonDetailsDeathdayValue.visible()
        viewPersonDetailsDeathdayValue.text = item.dateFormat?.format(date)?.capitalizeWords()
      }
      viewPersonDetailsProgress.visibleIf(item.isLoading)
    }
    renderImage(item.person)
  }

  private fun renderImage(person: Person) {
    with(binding) {
      Glide.with(this@PersonDetailsInfoView).clear(viewPersonDetailsImage)

      if (person.imagePath.isNullOrBlank()) {
        viewPersonDetailsImage.gone()
        viewPersonDetailsPlaceholder.visible()
        return
      }

      viewPersonDetailsImage.visible()
      viewPersonDetailsPlaceholder.gone()

      Glide
        .with(this@PersonDetailsInfoView)
        .load("${Config.TMDB_IMAGE_BASE_ACTOR_URL}${person.imagePath}")
        .transform(CenterCrop(), GranularRoundedCorners(topLeftCornerRadius, cornerRadius, cornerRadius, cornerRadius))
        .transition(DrawableTransitionOptions.withCrossFade(Config.IMAGE_FADE_DURATION_MS))
        .withFailListener {
          viewPersonDetailsImage.gone()
          viewPersonDetailsPlaceholder.visible()
        }.into(viewPersonDetailsImage)
    }
  }
}
