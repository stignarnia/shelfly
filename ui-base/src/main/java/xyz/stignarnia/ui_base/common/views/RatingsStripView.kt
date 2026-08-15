package xyz.stignarnia.ui_base.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import xyz.stignarnia.common.Config.SPOILERS_RATINGS_HIDE_SYMBOL
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.databinding.ViewRatingsStripBinding
import xyz.stignarnia.ui_base.utilities.extensions.colorFromAttr
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_model.Ratings

class RatingsStripView : LinearLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewRatingsStripBinding.inflate(LayoutInflater.from(context), this)

  var onTmdbClick: ((Ratings) -> Unit)? = null
  var onImdbClick: ((Ratings) -> Unit)? = null
  var onMetaClick: ((Ratings) -> Unit)? = null
  var onRottenClick: ((Ratings) -> Unit)? = null

  /** Invoked instead of the per-value callbacks while no OMDb key is set. */
  var onOmdbKeyMissingClick: (() -> Unit)? = null

  private val colorPrimary by lazy { context.colorFromAttr(android.R.attr.textColorPrimary) }
  private val colorSecondary by lazy { context.colorFromAttr(android.R.attr.textColorSecondary) }

  private lateinit var ratings: Ratings

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    orientation = HORIZONTAL
    gravity = Gravity.TOP
  }

  fun bind(ratings: Ratings) {
    this.ratings = ratings
    with(binding) {
      // TMDB is unaffected by a missing OMDb key - it comes from the catalog.
      bindValue(
        ratingsValue = ratings.tmdb,
        layoutView = viewRatingsStripTmdb,
        valueView = viewRatingsStripTmdbValue,
        progressView = viewRatingsStripTmdbProgress,
        linkView = viewRatingsStripTmdbLinkIcon,
        isHidden = ratings.isHidden,
        isTapToReveal = ratings.isTapToReveal,
        isOmdbKeyMissing = false,
        callback = onTmdbClick,
      )
      bindValue(
        ratingsValue = ratings.imdb,
        layoutView = viewRatingsStripImdb,
        valueView = viewRatingsStripImdbValue,
        progressView = viewRatingsStripImdbProgress,
        linkView = viewRatingsStripImdbLinkIcon,
        isHidden = ratings.isHidden,
        isTapToReveal = ratings.isTapToReveal,
        isOmdbKeyMissing = ratings.isOmdbKeyMissing,
        callback = onImdbClick,
      )
      bindValue(
        ratingsValue = ratings.metascore,
        layoutView = viewRatingsStripMeta,
        valueView = viewRatingsStripMetaValue,
        progressView = viewRatingsStripMetaProgress,
        linkView = viewRatingsStripMetaLinkIcon,
        isHidden = ratings.isHidden,
        isTapToReveal = ratings.isTapToReveal,
        isOmdbKeyMissing = ratings.isOmdbKeyMissing,
        callback = onMetaClick,
      )
      bindValue(
        ratingsValue = ratings.rottenTomatoes,
        layoutView = viewRatingsStripRotten,
        valueView = viewRatingsStripRottenValue,
        progressView = viewRatingsStripRottenProgress,
        linkView = viewRatingsStripRottenLinkIcon,
        isHidden = ratings.isHidden,
        isTapToReveal = ratings.isTapToReveal,
        isOmdbKeyMissing = ratings.isOmdbKeyMissing,
        callback = onRottenClick,
      )
    }
  }

  private fun bindValue(
    ratingsValue: Ratings.Value?,
    layoutView: View,
    valueView: TextView,
    progressView: View,
    linkView: ImageView,
    isHidden: Boolean,
    isTapToReveal: Boolean,
    isOmdbKeyMissing: Boolean,
    callback: ((Ratings) -> Unit)?,
  ) {
    val rating = ratingsValue?.value
    // A missing key is terminal, so never leave the slot spinning on it.
    val isLoading = ratingsValue?.isLoading == true && !isOmdbKeyMissing
    with(valueView) {
      visibleIf(!isLoading && !rating.isNullOrBlank(), gone = false)
      text = if (isHidden) {
        tag = rating
        SPOILERS_RATINGS_HIDE_SYMBOL
      } else {
        rating
      }
      setTextColor(if (rating != null) colorPrimary else colorSecondary)
    }

    with(layoutView) {
      if (isOmdbKeyMissing) {
        onClick { onOmdbKeyMissingClick?.invoke() }
      } else if (isHidden && isTapToReveal && !rating.isNullOrBlank()) {
        onClick {
          valueView.tag?.let { valueView.text = it.toString() }
          onClick {
            if (!ratings.isAnyLoading()) {
              callback?.invoke(ratings)
            }
          }
        }
      } else {
        onClick {
          if (!ratings.isAnyLoading()) {
            callback?.invoke(ratings)
          }
        }
      }
    }

    progressView.visibleIf(isLoading)
    with(linkView) {
      visibleIf(!isLoading && rating.isNullOrBlank())
      setImageResource(if (isOmdbKeyMissing) R.drawable.ic_close else R.drawable.ic_link)
    }
  }

  fun isBound() = this::ratings.isInitialized && !this.ratings.isAnyLoading()
}
