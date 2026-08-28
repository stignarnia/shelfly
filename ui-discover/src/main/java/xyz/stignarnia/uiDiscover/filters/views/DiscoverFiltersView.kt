package xyz.stignarnia.uiDiscover.filters.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.children
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiDiscover.R
import xyz.stignarnia.uiDiscover.databinding.ViewDiscoverFiltersBinding
import xyz.stignarnia.uiModel.DiscoverFeed
import xyz.stignarnia.uiModel.DiscoverFeed.ANTICIPATED
import xyz.stignarnia.uiModel.DiscoverFeed.POPULAR
import xyz.stignarnia.uiModel.DiscoverFeed.RECENT
import xyz.stignarnia.uiModel.DiscoverFeed.TRENDING
import xyz.stignarnia.uiModel.DiscoverFilters
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.StreamingProvider

class DiscoverFiltersView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewDiscoverFiltersBinding.inflate(LayoutInflater.from(context), this)

  var onFeedChipClick: (() -> Unit)? = null
  var onGenresChipClick: (() -> Unit)? = null
  var onProvidersChipClick: (() -> Unit)? = null
  var onHideCollectionChipClick: (() -> Unit)? = null

  private lateinit var filters: DiscoverFilters

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    with(binding) {
      discoverGenresChip.text = discoverGenresChip.text.toString().filter { it.isLetter() }
      discoverGenresChip.onClick { onGenresChipClick?.invoke() }
      discoverProvidersChip.onClick { onProvidersChipClick?.invoke() }
      discoverFeedChip.onClick { onFeedChipClick?.invoke() }
      discoverCollectionChip.onClick { onHideCollectionChipClick?.invoke() }
    }
  }

  fun bind(filters: DiscoverFilters) {
    this.filters = filters
    bindFeed(filters.feedOrder)
    bindGenres(filters.genres)
    bindProviders(filters.providers)
    with(binding) {
      discoverCollectionChip.isChecked = filters.hideCollection
    }
  }

  private fun bindFeed(feed: DiscoverFeed) {
    with(binding) {
      discoverFeedChip.text =
        when (feed) {
          TRENDING -> context.getString(R.string.textFeedTrending)
          POPULAR -> context.getString(R.string.textFeedPopular)
          ANTICIPATED -> context.getString(R.string.textFeedAnticipated)
          RECENT -> context.getString(R.string.textSortNewest)
        }
    }
  }

  private fun bindGenres(genres: List<Genre>) {
    with(binding) {
      discoverGenresChip.isSelected = genres.isNotEmpty()
      discoverGenresChip.text =
        when {
          genres.isEmpty() -> {
            context.getString(R.string.textGenres).filter { it.isLetter() }
          }

          genres.size == 1 -> {
            context.getString(genres.first().displayName)
          }

          genres.size == 2 -> {
            "${context.getString(genres[0].displayName)}, ${context.getString(genres[1].displayName)}"
          }

          else -> {
            "${
              context.getString(
                genres[0].displayName,
              )
            }, ${context.getString(genres[1].displayName)} + ${genres.size - 2}"
          }
        }
    }
  }

  private fun bindProviders(providers: List<StreamingProvider>) {
    with(binding) {
      discoverProvidersChip.isSelected = providers.isNotEmpty()
      discoverProvidersChip.text =
        when {
          providers.isEmpty() -> {
            context.getString(R.string.textDiscoverFilterProviders)
          }

          providers.size == 1 -> {
            providers[0].name
          }

          else -> {
            context.getString(
              R.string.textDiscoverFilterProvidersCount,
              providers[0].name,
              providers.size - 1,
            )
          }
        }
    }
  }

  override fun setEnabled(enabled: Boolean) {
    binding.discoverChips.children.forEach { it.isEnabled = enabled }
  }
}
