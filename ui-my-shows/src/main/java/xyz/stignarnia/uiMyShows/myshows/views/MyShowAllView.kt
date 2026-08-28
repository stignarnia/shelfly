package xyz.stignarnia.uiMyShows.myshows.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import com.bumptech.glide.Glide
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.Config.SPOILERS_HIDE_SYMBOL
import xyz.stignarnia.common.Config.SPOILERS_REGEX
import xyz.stignarnia.uiBase.common.views.ShowView
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.onLongClick
import xyz.stignarnia.uiBase.utilities.extensions.setOutboundRipple
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiMyShows.R
import xyz.stignarnia.uiMyShows.databinding.ViewCollectionShowBinding
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem
import java.util.Locale.ENGLISH

class MyShowAllView : ShowView<MyShowsItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewCollectionShowBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

    clipChildren = false
    clipToPadding = false

    with(binding) {
      collectionShowRoot.onClick { itemClickListener?.invoke(item) }
      collectionShowRoot.onLongClick { itemLongClickListener?.invoke(item) }
      collectionShowRoot.setOutboundRipple(
        size = (context.dimenToPx(R.dimen.collectionItemRippleSpace)).toFloat(),
        corner = context.dimenToPx(R.dimen.mediaTileCorner).toFloat(),
      )
    }

    imageLoadCompleteListener = { loadTranslation() }
  }

  override val imageView: ImageView = binding.collectionShowImage
  override val placeholderView: ImageView = binding.collectionShowPlaceholder

  private lateinit var item: MyShowsItem

  override fun bind(item: MyShowsItem) {
    clear()
    this.item = item

    with(binding) {
      collectionShowProgress.visibleIf(item.isLoading)
      collectionShowTitle.text =
        if (item.translation?.title.isNullOrBlank()) {
          item.show.title
        } else {
          item.translation.title
        }

      bindDescription(item)
      bindRating(item)

      collectionShowNetwork.text =
        if (item.show.year > 0) {
          context.getString(R.string.textNetwork, item.show.network, item.show.year.toString())
        } else {
          String.format("%s", item.show.network)
        }

      collectionShowNetwork.visibleIf(item.show.network.isNotBlank())

      item.userRating?.let {
        collectionShowUserStarIcon.visible()
        collectionShowUserRating.visible()
        collectionShowUserRating.text = String.format(ENGLISH, "%d", it)
      }
    }
    loadImage(item)
  }

  private fun bindDescription(item: MyShowsItem) {
    with(binding) {
      var description =
        if (item.translation?.overview.isNullOrBlank()) {
          item.show.overview
        } else {
          item.translation.overview
        }

      if (item.spoilers.isSpoilerHidden) {
        collectionShowDescription.tag = description
        description = SPOILERS_REGEX.replace(description, SPOILERS_HIDE_SYMBOL)

        if (item.spoilers.isSpoilerTapToReveal) {
          collectionShowDescription.onClick { view ->
            view.tag?.let { collectionShowDescription.text = it.toString() }
            view.isClickable = false
          }
        }
      }

      collectionShowDescription.text = description
      collectionShowDescription.visibleIf(item.show.overview.isNotBlank())
    }
  }

  private fun bindRating(item: MyShowsItem) {
    with(binding) {
      var rating = String.format(ENGLISH, "%.1f", item.show.rating)

      if (item.spoilers.isSpoilerRatingsHidden) {
        collectionShowRating.tag = rating
        rating = Config.SPOILERS_RATINGS_HIDE_SYMBOL

        if (item.spoilers.isSpoilerTapToReveal) {
          collectionShowRating.onClick { view ->
            view.tag?.let { collectionShowRating.text = it.toString() }
            view.isClickable = false
          }
        }
      }

      collectionShowRating.text = rating
    }
  }

  private fun loadTranslation() {
    if (item.translation == null) {
      missingTranslationListener?.invoke(item)
    }
  }

  private fun clear() {
    with(binding) {
      collectionShowTitle.text = ""
      collectionShowDescription.text = ""
      collectionShowNetwork.text = ""
      collectionShowRating.text = ""
      collectionShowPlaceholder.gone()
      collectionShowUserStarIcon.gone()
      collectionShowUserRating.gone()
      Glide.with(this@MyShowAllView).clear(collectionShowImage)
    }
  }
}
