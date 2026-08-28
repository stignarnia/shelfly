package xyz.stignarnia.uiEpisodes.details.links

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.openWebUrl
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiEpisodes.R
import xyz.stignarnia.uiEpisodes.databinding.ViewEpisodeLinksBinding
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiNavigation.java.NavigationArgs

@AndroidEntryPoint
class EpisodeLinksBottomSheet : BaseBottomSheetFragment(R.layout.view_episode_links) {
  @Parcelize
  data class Options(
    val showIds: Ids,
    val ids: Ids,
    val season: Int,
    val episodeNumber: Int,
  ) : Parcelable

  companion object {
    fun createBundle(
      showIds: Ids,
      episode: Episode,
    ): Bundle {
      val options =
        Options(
          showIds = showIds,
          ids = episode.ids,
          season = episode.season,
          episodeNumber = episode.number,
        )
      return Bundle().apply { putParcelable(NavigationArgs.ARG_OPTIONS, options) }
    }
  }

  private val binding by viewBinding(ViewEpisodeLinksBinding::bind)

  private val options by lazy { requireParcelable<Options>(NavigationArgs.ARG_OPTIONS) }
  private val showIds by lazy { options.showIds }
  private val ids by lazy { options.ids }
  private val season by lazy { options.season }
  private val episodeNumber by lazy { options.episodeNumber }

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
  }

  private fun setupView() {
    with(binding) {
      val searchQuery = "${showIds.slug.id} season $season episode $episodeNumber"
      viewEpisodeLinksGoogle.onClick {
        openWebUrl("https://www.google.com/search?q=$searchQuery")
      }

      viewEpisodeLinksDuckDuck.onClick {
        openWebUrl("https://duckduckgo.com/?q=$searchQuery")
      }

      viewEpisodeLinksButtonClose.onClick {
        closeSheet()
      }
    }

    setTmdbLink()
    setTvdbLink()
    setImdbLink()
  }

  private fun setTmdbLink() {
    binding.viewEpisodeLinksTmdb.run {
      if (ids.tmdb.id == -1L) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          openWebUrl("https://www.themoviedb.org/tv/${showIds.tmdb.id}/season/$season/episode/$episodeNumber")
        }
      }
    }
  }

  private fun setTvdbLink() {
    binding.viewEpisodeLinksTvdb.run {
      if (ids.tvdb.id == -1L) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          openWebUrl("https://thetvdb.com/series/${showIds.tvdb.id}/episodes/${ids.tvdb.id}")
        }
      }
    }
  }

  private fun setImdbLink() {
    binding.viewEpisodeLinksImdb.run {
      if (ids.imdb.id.isBlank()) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          val i = Intent(Intent.ACTION_VIEW)
          i.data = "imdb:///title/${ids.imdb.id}".toUri()
          try {
            startActivity(i)
          } catch (e: ActivityNotFoundException) {
            // IMDb App not installed.
            // Start in web browser
            openWebUrl("https://www.imdb.com/title/${ids.imdb.id}")
          }
        }
      }
    }
  }
}
