package xyz.stignarnia.uiMyShows.common.views

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
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.uiBase.common.views.ShowView
import xyz.stignarnia.uiBase.utilities.extensions.capitalizeWords
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.onLongClick
import xyz.stignarnia.uiBase.utilities.extensions.setOutboundRipple
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiMyShows.R
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem
import xyz.stignarnia.uiMyShows.databinding.ViewCollectionShowBinding
import java.util.Locale.ENGLISH

class CollectionShowView : ShowView<CollectionListItem.ShowItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewCollectionShowBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

    clipChildren = false
    clipToPadding = false

    with(binding.collectionShowRoot) {
      onClick { itemClickListener?.invoke(item) }
      onLongClick { itemLongClickListener?.invoke(item) }
      setOutboundRipple(
        size = (context.dimenToPx(R.dimen.collectionItemRippleSpace)).toFloat(),
        corner = context.dimenToPx(R.dimen.mediaTileCorner).toFloat(),
      )
    }

    imageLoadCompleteListener = { loadTranslation() }
  }

  override val imageView: ImageView = binding.collectionShowImage
  override val placeholderView: ImageView = binding.collectionShowPlaceholder

  private var nowUtc = nowUtc()

  private lateinit var item: CollectionListItem.ShowItem

  override fun bind(item: CollectionListItem.ShowItem) {
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

      with(collectionShowReleaseDate) {
        val releaseDate = item.getReleaseDate()
        if (releaseDate != null) {
          visibleIf(releaseDate.isAfter(nowUtc))
          text = item.dateFormat.format(releaseDate).capitalizeWords()
        } else {
          gone()
        }
      }

      item.userRating?.let {
        collectionShowUserStarIcon.visible()
        collectionShowUserRating.visible()
        collectionShowUserRating.text = String.format(ENGLISH, "%d", it)
      }
    }

    loadImage(item)
  }

  private fun bindDescription(item: CollectionListItem.ShowItem) {
    with(binding) {
      collectionShowDescription.text =
        if (item.translation?.overview.isNullOrBlank()) {
          item.show.overview
        } else {
          item.translation.overview
        }

      if (item.spoilers.isSpoilerHidden) {
        collectionShowDescription.tag = collectionShowDescription.text
        collectionShowDescription.text =
          SPOILERS_REGEX.replace(collectionShowDescription.text, SPOILERS_HIDE_SYMBOL)

        if (item.spoilers.isSpoilerTapToReveal) {
          collectionShowDescription.onClick { view ->
            view.tag?.let { collectionShowDescription.text = it.toString() }
            view.isClickable = false
          }
        }
      }

      collectionShowDescription.visibleIf(item.show.overview.isNotBlank())
    }
  }

  private fun bindRating(item: CollectionListItem.ShowItem) {
    var rating = String.format(ENGLISH, "%.1f", item.show.rating)

    with(binding) {
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
      collectionShowUserRating.gone()
      collectionShowUserStarIcon.gone()
      Glide.with(this@CollectionShowView).clear(collectionShowImage)
    }
  }
}
