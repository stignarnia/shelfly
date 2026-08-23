package xyz.stignarnia.ui_my_shows.myshows.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_model.MyShowsSection
import xyz.stignarnia.ui_model.MyShowsSection.ALL
import xyz.stignarnia.ui_model.MyShowsSection.RECENTS
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_my_shows.R
import xyz.stignarnia.ui_my_shows.databinding.ViewMyShowsHeaderBinding
import xyz.stignarnia.ui_my_shows.myshows.recycler.MyShowsItem
import java.util.Locale.ENGLISH

class MyShowHeaderView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewMyShowsHeaderBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    clipChildren = false
    clipToPadding = false
  }

  fun bind(
    item: MyShowsItem.Header,
    viewMode: ListViewMode,
    typeClickListener: (() -> Unit)?,
    sortClickListener: ((MyShowsSection, SortOrder, SortType) -> Unit)?,
    networksClickListener: (() -> Unit)?,
    genresClickListener: (() -> Unit)?,
  ) {
    bindLabel(item)
    with(binding) {
      myShowsFilterChipsScroll.visibleIf(item.section != RECENTS)
      myShowsSortChip.visibleIf(item.sortOrder != null)
      myShowsNetworksChip.visibleIf(item.networks != null)
      myShowsGenresChip.visibleIf(item.genres != null)

      with(myShowsTypeChip) {
        isSelected = item.section != ALL
        text = context.getString(item.section.displayString)
        visibleIf(item.section != RECENTS)
        onClick { typeClickListener?.invoke() }
      }

      item.sortOrder?.let { sortOrder ->
        myShowsSortChip.text = context.getString(sortOrder.first.displayString)
        myShowsSortChip.onClick {
          sortClickListener?.invoke(item.section, sortOrder.first, sortOrder.second)
        }
        val sortIcon = when (sortOrder.second) {
          SortType.ASCENDING -> R.drawable.ic_arrow_alt_up
          SortType.DESCENDING -> R.drawable.ic_arrow_alt_down
        }
        myShowsSortChip.closeIcon = ContextCompat.getDrawable(context, sortIcon)
      }

      item.networks?.let { networks ->
        myShowsNetworksChip.isSelected = networks.isNotEmpty()
        myShowsNetworksChip.onClick { networksClickListener?.invoke() }
        myShowsNetworksChip.text = when {
          networks.isEmpty() -> context.getString(R.string.textDiscoverFilterProviders)
          networks.size == 1 -> networks[0]
          else -> context.getString(
            R.string.textDiscoverFilterProvidersCount,
            networks[0],
            networks.size - 1,
          )
        }
      }

      item.genres?.let { genres ->
        myShowsGenresChip.isSelected = genres.isNotEmpty()
        myShowsGenresChip.onClick { genresClickListener?.invoke() }
        myShowsGenresChip.text = when {
          genres.isEmpty() -> context.getString(R.string.textGenres).filter { it.isLetter() }
          genres.size == 1 -> context.getString(genres.first().displayName)
          genres.size == 2 -> "${context.getString(genres[0].displayName)}, ${context.getString(genres[1].displayName)}"
          else -> "${context.getString(genres[0].displayName)}, " +
            "${context.getString(genres[1].displayName)} + ${genres.size - 2}"
        }
      }
    }
  }

  private fun bindLabel(item: MyShowsItem.Header) {
    val headerLabel = context.getString(item.section.displayString)
    binding.myShowsHeaderLabel.text = when (item.section) {
      RECENTS -> headerLabel
      else -> String.format(ENGLISH, "%s (%d)", headerLabel, item.itemCount)
    }
  }
}
