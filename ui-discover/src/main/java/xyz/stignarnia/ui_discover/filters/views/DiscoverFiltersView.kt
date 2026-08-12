package xyz.stignarnia.ui_discover.filters.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.children
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_discover.R
import xyz.stignarnia.ui_discover.databinding.ViewDiscoverFiltersBinding
import xyz.stignarnia.ui_model.DiscoverFeed
import xyz.stignarnia.ui_model.DiscoverFeed.ANTICIPATED
import xyz.stignarnia.ui_model.DiscoverFeed.POPULAR
import xyz.stignarnia.ui_model.DiscoverFeed.RECENT
import xyz.stignarnia.ui_model.DiscoverFeed.TRENDING
import xyz.stignarnia.ui_model.DiscoverFilters
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_model.Network

class DiscoverFiltersView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewDiscoverFiltersBinding.inflate(LayoutInflater.from(context), this)

  var onFeedChipClick: (() -> Unit)? = null
  var onGenresChipClick: (() -> Unit)? = null
  var onNetworksChipClick: (() -> Unit)? = null
  var onHideCollectionChipClick: (() -> Unit)? = null

  private lateinit var filters: DiscoverFilters

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    with(binding) {
      discoverGenresChip.text = discoverGenresChip.text.toString().filter { it.isLetter() }
      discoverGenresChip.onClick { onGenresChipClick?.invoke() }
      discoverNetworksChip.text = discoverNetworksChip.text.toString().filter { it.isLetter() }
      discoverNetworksChip.onClick { onNetworksChipClick?.invoke() }
      discoverFeedChip.onClick { onFeedChipClick?.invoke() }
      discoverCollectionChip.onClick { onHideCollectionChipClick?.invoke() }
    }
  }

  fun bind(filters: DiscoverFilters) {
    this.filters = filters
    bindFeed(filters.feedOrder)
    bindGenres(filters.genres)
    bindNetworks(filters.networks)
    with(binding) {
      discoverCollectionChip.isChecked = filters.hideCollection
    }
  }

  private fun bindFeed(feed: DiscoverFeed) {
    with(binding) {
      discoverFeedChip.text = when (feed) {
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
      discoverGenresChip.text = when {
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

  private fun bindNetworks(networks: List<Network>) {
    with(binding) {
      discoverNetworksChip.isSelected = networks.isNotEmpty()
      discoverNetworksChip.text = when {
        networks.isEmpty() -> context.getString(R.string.textNetworks).filter { it.isLetter() }
        networks.size == 1 -> networks[0].channels.first()
        else -> throw IllegalStateException()
      }
    }
  }

  override fun setEnabled(enabled: Boolean) {
    binding.discoverChips.children.forEach { it.isEnabled = enabled }
  }
}
