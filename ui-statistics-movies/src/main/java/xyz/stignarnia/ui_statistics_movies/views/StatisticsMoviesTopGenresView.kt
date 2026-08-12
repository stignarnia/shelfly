package xyz.stignarnia.ui_statistics_movies.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.google.android.material.card.MaterialCardView
import xyz.stignarnia.ui_base.utilities.extensions.colorFromAttr
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_statistics_movies.R
import xyz.stignarnia.ui_statistics_movies.databinding.ViewStatisticsMoviesCardTopGenreBinding

@SuppressLint("SetTextI18n")
class StatisticsMoviesTopGenresView : MaterialCardView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewStatisticsMoviesCardTopGenreBinding.inflate(LayoutInflater.from(context), this)

  private var topGenres = emptyList<Genre>()

  init {
    layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    setCardBackgroundColor(context.colorFromAttr(R.attr.colorCardBackground))
    cardElevation = context.dimenToPx(R.dimen.elevationSmall).toFloat()
    strokeWidth = 0
    onClick {
      showGenres(10)
      isClickable = false
      binding.viewMoviesTopGenresSubValue.text = context.getString(R.string.textStatisticsMoviesTopGenreSubValue2)
    }
  }

  fun bind(genres: List<Genre>) {
    topGenres = genres.toList()
    showGenres(3)
  }

  private fun showGenres(limit: Int) {
    binding.viewMoviesTopGenresValue.text = topGenres
      .take(limit)
      .joinToString("\n") {
        context.getString(it.displayName)
      }
  }
}
