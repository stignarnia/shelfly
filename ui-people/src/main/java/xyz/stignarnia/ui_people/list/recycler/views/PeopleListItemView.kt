package xyz.stignarnia.ui_people.list.recycler.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import xyz.stignarnia.common.Config
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.gone
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visible
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.extensions.withFailListener
import xyz.stignarnia.ui_base.utilities.extensions.withSuccessListener
import xyz.stignarnia.ui_model.Person
import xyz.stignarnia.ui_model.Person.Department
import xyz.stignarnia.ui_model.Person.Job
import xyz.stignarnia.ui_people.R
import xyz.stignarnia.ui_people.databinding.ViewPeopleListItemBinding
import xyz.stignarnia.ui_people.list.recycler.PeopleListItem

class PeopleListItemView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewPeopleListItemBinding.inflate(LayoutInflater.from(context), this)

  var onItemClickListener: ((Person) -> Unit)? = null

  private val cornerRadius by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  private val centerCropTransformation by lazy { CenterCrop() }
  private val cornersTransformation by lazy { RoundedCorners(cornerRadius) }

  private lateinit var item: PeopleListItem.PersonItem

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    binding.viewPersonItemRoot.onClick { onItemClickListener?.invoke(item.person) }
  }

  fun bind(item: PeopleListItem.PersonItem) {
    clear()
    this.item = item

    with(binding) {
      viewPersonItemTitle.text = item.person.name
      val mainJob = item.person.jobs.firstOrNull { it != Job.UNKNOWN }
      viewPersonItemHeader.text = when (mainJob) {
        Job.DIRECTOR -> context.getString(R.string.textDirector)
        Job.WRITER, Job.STORY -> context.getString(R.string.textWriting)
        Job.SCREENPLAY -> context.getString(R.string.textScreenplay)
        Job.MUSIC, Job.ORIGINAL_MUSIC -> context.getString(R.string.textMusic)
        else -> when (item.person.department) {
          Department.ACTING -> context.getString(R.string.textActing)
          Department.DIRECTING -> context.getString(R.string.textDirector)
          Department.WRITING -> context.getString(R.string.textWriting)
          Department.SOUND -> context.getString(R.string.textMusic)
          Department.UNKNOWN -> "-"
        }
      }
      viewPersonItemDescription.visibleIf(item.person.episodesCount > 0)
      viewPersonItemDescription.text =
        context.getString(
          R.string.textEpisodesCount,
          context.getString(R.string.textEpisodes),
          item.person.episodesCount,
        )
    }

    loadImage(item.person.imagePath)
  }

  private fun loadImage(imagePath: String?) {
    with(binding) {
      if (imagePath.isNullOrBlank()) {
        viewPersonItemImage.gone()
        viewPersonItemPlaceholder.visible()
        return
      }
      Glide
        .with(this@PeopleListItemView)
        .load("${Config.TMDB_IMAGE_BASE_PROFILE_THUMB_URL}$imagePath")
        .transform(centerCropTransformation, cornersTransformation)
        .transition(DrawableTransitionOptions.withCrossFade(Config.IMAGE_FADE_DURATION_MS))
        .withSuccessListener {
          viewPersonItemPlaceholder.gone()
        }.withFailListener {
          viewPersonItemImage.gone()
          viewPersonItemPlaceholder.fadeIn(Config.IMAGE_FADE_DURATION_MS.toLong())
        }.into(viewPersonItemImage)
    }
  }

  private fun clear() {
    with(binding) {
      viewPersonItemImage.visible()
      viewPersonItemPlaceholder.gone()
      Glide.with(this@PeopleListItemView).clear(viewPersonItemImage)
    }
  }
}
