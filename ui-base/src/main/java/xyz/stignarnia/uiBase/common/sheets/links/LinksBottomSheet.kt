package xyz.stignarnia.uiBase.common.sheets.links

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import xyz.stignarnia.common.Mode
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.databinding.ViewLinksBinding
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.openWebUrl
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiNavigation.java.NavigationArgs

@AndroidEntryPoint
class LinksBottomSheet : BaseBottomSheetFragment(R.layout.view_links) {
  @Parcelize
  data class Options(
    val ids: Ids,
    val title: String,
    val website: String,
    val type: Mode,
  ) : Parcelable

  companion object {
    fun createBundle(movie: Movie): Bundle {
      val options = Options(movie.ids, movie.title, movie.homepage, Mode.MOVIES)
      return Bundle().apply { putParcelable(NavigationArgs.ARG_OPTIONS, options) }
    }

    fun createBundle(show: Show): Bundle {
      val options = Options(show.ids, show.title, show.homepage, Mode.SHOWS)
      return Bundle().apply { putParcelable(NavigationArgs.ARG_OPTIONS, options) }
    }
  }

  private val viewModel by viewModels<LinksViewModel>()
  private val binding by viewBinding(ViewLinksBinding::bind)

  private val options by lazy { requireParcelable<Options>(NavigationArgs.ARG_OPTIONS) }
  private val ids by lazy { options.ids }
  private val title by lazy { options.title }
  private val website by lazy { options.website }
  private val type by lazy { options.type }

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
      viewLinksJustWatch.onClick {
        val country = viewModel.loadCountry()
        openWebUrl(
          "https://www.justwatch.com/${country.code}/${country.justWatchQuery}?content_type=${type.type}&q=${
            Uri.encode(
              title,
            )
          }",
        )
      }
      viewLinksYouTube.onClick {
        openWebUrl("https://www.youtube.com/results?search_query=${getQuery()}")
      }
      viewLinksWiki.onClick {
        openWebUrl("https://en.wikipedia.org/w/index.php?search=${getQuery()}")
      }
      viewLinksGoogle.onClick {
        openWebUrl("https://www.google.com/search?q=${getQuery()}")
      }
      viewLinksDuckDuck.onClick {
        openWebUrl("https://duckduckgo.com/?q=${getQuery()}")
      }
      viewLinksGif.onClick {
        openWebUrl("https://giphy.com/search/${getQuery()}")
      }
      viewLinksTwitter.onClick {
        openWebUrl("https://twitter.com/search?q=${getQuery()}&src=typed_query")
      }
      viewLinksButtonClose.onClick { closeSheet() }
    }
    setWebLink()
    setTvdbLink()
    setTmdbLink()
    setImdbLink()
  }

  private fun setWebLink() {
    binding.viewLinksWebsite.run {
      if (website.isBlank()) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick { openWebUrl(website) }
      }
    }
  }

  private fun setTvdbLink() {
    binding.viewLinksTvdb.run {
      if (ids.tvdb.id == -1L) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          when (type) {
            Mode.SHOWS -> openWebUrl("https://www.thetvdb.com/?id=${ids.tvdb.id}&tab=series")
            Mode.MOVIES -> openWebUrl("https://www.thetvdb.com/?id=${ids.tvdb.id}&tab=movies")
          }
        }
      }
    }
  }

  private fun setTmdbLink() {
    binding.viewLinksTmdb.run {
      if (ids.tmdb.id == -1L) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          when (type) {
            Mode.SHOWS -> openWebUrl("https://www.themoviedb.org/tv/${ids.tmdb.id}")
            Mode.MOVIES -> openWebUrl("https://www.themoviedb.org/movie/${ids.tmdb.id}")
          }
        }
      }
    }
  }

  private fun setImdbLink() {
    binding.viewLinksImdb.run {
      if (ids.imdb.id.isBlank()) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          val i = Intent(Intent.ACTION_VIEW, "imdb:///title/${ids.imdb.id}".toUri())
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

  private fun getQuery() =
    when (type) {
      Mode.SHOWS -> "$title TV Series"
      Mode.MOVIES -> "$title Movie"
    }
}
