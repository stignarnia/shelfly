package xyz.stignarnia.uiLists.details.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.Config.SPOILERS_RATINGS_HIDE_SYMBOL
import xyz.stignarnia.common.Config.SPOILERS_REGEX
import xyz.stignarnia.uiBase.utilities.extensions.colorFromAttr
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.expandTouch
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.setOutboundRipple
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiLists.R
import xyz.stignarnia.uiLists.databinding.ViewListDetailsMovieItemBinding
import xyz.stignarnia.uiLists.details.recycler.ListDetailsItem
import xyz.stignarnia.uiModel.Movie
import java.util.Locale.ENGLISH
import kotlin.math.abs

class ListDetailsMovieItemView : ListDetailsItemView {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewListDetailsMovieItemBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    setBackgroundColor(context.colorFromAttr(android.R.attr.windowBackground))

    clipChildren = false
    clipToPadding = false

    imageLoadCompleteListener = {
      if (item.translation == null) {
        missingTranslationListener?.invoke(item)
      }
    }

    with(binding) {
      listDetailsMovieHandle.expandTouch(100)
      // The handle carries no click listener; performClick is called so the drag affordance still reports itself to accessibility services.
      listDetailsMovieHandle.setOnTouchListener { view, event ->
        if (item.isManageMode && event.action == ACTION_DOWN) {
          itemDragStartListener?.invoke()
        }
        if (event.action == ACTION_UP) view.performClick()
        false
      }

      var x = 0F
      // The tap is performed here rather than left to the framework: this listener consumes the gesture once it turns into a swipe, so ownership of the click has to sit in one place to avoid firing it twice.
      listDetailsMovieRoot.setOnTouchListener { view, event ->
        if (item.isManageMode) {
          return@setOnTouchListener false
        }
        if (event.action == ACTION_DOWN) x = event.x
        if (event.action == ACTION_MOVE && abs(x - event.x) > 50F) {
          itemSwipeStartListener?.invoke()
          return@setOnTouchListener true
        }
        if (event.action == ACTION_UP) {
          x = 0F
          return@setOnTouchListener view.performClick()
        }
        false
      }

      listDetailsMovieRoot.onClick {
        if (item.isEnabled && !item.isManageMode) itemClickListener?.invoke(item)
      }
      listDetailsMovieRoot.setOutboundRipple(
        size = (context.dimenToPx(R.dimen.collectionItemRippleSpace)).toFloat(),
        corner = context.dimenToPx(R.dimen.mediaTileCorner).toFloat(),
      )
    }
  }

  override val imageView: ImageView = binding.listDetailsMovieImage
  override val placeholderView: ImageView = binding.listDetailsMoviePlaceholder

  override fun bind(item: ListDetailsItem) {
    super.bind(item)

    with(binding) {
      Glide.with(this@ListDetailsMovieItemView).clear(listDetailsMovieImage)
      val movie = item.requireMovie()

      listDetailsMovieProgress.visibleIf(item.isLoading)

      listDetailsMovieTitle.text =
        if (item.translation?.title.isNullOrBlank()) {
          movie.title
        } else {
          item.translation.title
        }

      bindDescription(item, movie)
      bindRating(item, movie)

      listDetailsMovieHeader.text = String.format(ENGLISH, "%d", movie.year)
      listDetailsMovieUserRating.text = String.format(ENGLISH, "%d", item.userRating)

      listDetailsMovieRank.visibleIf(item.isRankDisplayed)
      listDetailsMovieRank.text = String.format(ENGLISH, "%d", item.rankDisplay)

      listDetailsMovieHandle.visibleIf(item.isManageMode)
      listDetailsMovieStarIcon.visibleIf(!item.isManageMode)
      listDetailsMovieUserStarIcon.visibleIf(!item.isManageMode && item.userRating != null)
      listDetailsMovieUserRating.visibleIf(!item.isManageMode && item.userRating != null)

      with(listDetailsMovieHeaderBadge) {
        val inCollection = item.isWatched || item.isWatchlist
        visibleIf(inCollection)
        if (inCollection) {
          val color = if (item.isWatched) R.color.colorAccent else R.color.colorGrayLight
          imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
        }
      }

      listDetailsMovieRoot.alpha = if (item.isEnabled) 1F else 0.45F
    }

    loadImage(item)
  }

  private fun bindDescription(
    item: ListDetailsItem,
    movie: Movie,
  ) {
    var description =
      when {
        item.translation?.overview.isNullOrBlank() -> {
          movie.overview.ifBlank {
            context.getString(R.string.textNoDescription)
          }
        }

        else -> {
          item.translation.overview
        }
      }

    val isMyHidden = item.spoilers.isMyMoviesHidden && item.isWatched
    val isWatchlistHidden = item.spoilers.isWatchlistMoviesHidden && item.isWatchlist
    val isNotCollectedHidden = item.spoilers.isNotCollectedMoviesHidden && (!item.isWatched && !item.isWatchlist)
    if (isMyHidden || isWatchlistHidden || isNotCollectedHidden) {
      binding.listDetailsMovieDescription.tag = description
      description = SPOILERS_REGEX.replace(description, Config.SPOILERS_HIDE_SYMBOL)
    }

    binding.listDetailsMovieDescription.text = description
    if (item.spoilers.isTapToReveal) {
      with(binding.listDetailsMovieDescription) {
        onClick {
          tag?.let { text = it.toString() }
          isClickable = false
        }
      }
    }
  }

  private fun bindRating(
    item: ListDetailsItem,
    movie: Movie,
  ) {
    var rating = String.format(ENGLISH, "%.1f", movie.rating)

    val isMyHidden = item.spoilers.isMyMoviesRatingsHidden && item.isWatched
    val isWatchlistHidden = item.spoilers.isWatchlistMoviesRatingsHidden && item.isWatchlist
    val isNotCollectedHidden = item.spoilers.isNotCollectedMoviesRatingsHidden && (!item.isWatched && !item.isWatchlist)
    if (isMyHidden || isWatchlistHidden || isNotCollectedHidden) {
      binding.listDetailsMovieRating.tag = rating
      rating = SPOILERS_RATINGS_HIDE_SYMBOL
    }

    binding.listDetailsMovieRating.visibleIf(!item.isManageMode)
    binding.listDetailsMovieRating.text = rating

    if (item.spoilers.isTapToReveal) {
      with(binding.listDetailsMovieRating) {
        onClick {
          tag?.let { text = it.toString() }
          isClickable = false
        }
      }
    }
  }
}
