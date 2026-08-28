package xyz.stignarnia.uiPeople.details.links

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
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.ViewPersonLinksBinding

@AndroidEntryPoint
class PersonLinksBottomSheet : BaseBottomSheetFragment(R.layout.view_person_links) {
  @Parcelize
  data class Options(
    val ids: Ids,
    val name: String,
    val website: String?,
  ) : Parcelable

  companion object {
    fun createBundle(person: Person): Bundle {
      val options = Options(person.ids, person.name, person.homepage)
      return Bundle().apply { putParcelable(NavigationArgs.ARG_OPTIONS, options) }
    }
  }

  private val binding by viewBinding(ViewPersonLinksBinding::bind)

  private val options by lazy { requireParcelable<Options>(NavigationArgs.ARG_OPTIONS) }
  private val ids by lazy { options.ids }
  private val name by lazy { options.name }
  private val website by lazy { options.website }

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
      viewPersonLinksYouTube.onClick {
        openWebUrl("https://www.youtube.com/results?search_query=$name")
      }
      viewPersonLinksWiki.onClick {
        openWebUrl("https://en.wikipedia.org/w/index.php?search=$name")
      }
      viewPersonLinksGoogle.onClick {
        openWebUrl("https://www.google.com/search?q=$name")
      }
      viewPersonLinksDuckDuck.onClick {
        openWebUrl("https://duckduckgo.com/?q=$name")
      }
      viewPersonLinksTwitter.onClick {
        openWebUrl("https://twitter.com/search?q=$name&src=typed_query&f=user")
      }
      viewPersonLinksButtonClose.onClick { closeSheet() }
    }
    setWebLink()
    setTmdbLink()
    setImdbLink()
  }

  private fun setWebLink() {
    binding.viewPersonLinksWebsite.run {
      if (website.isNullOrBlank()) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick { openWebUrl(website ?: "") }
      }
    }
  }

  private fun setTmdbLink() {
    binding.viewPersonLinksTmdb.run {
      if (ids.tmdb.id == -1L) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          openWebUrl("https://www.themoviedb.org/person/${ids.tmdb.id}")
        }
      }
    }
  }

  private fun setImdbLink() {
    binding.viewPersonLinksImdb.run {
      if (ids.imdb.id.isBlank()) {
        alpha = 0.5F
        isEnabled = false
      } else {
        onClick {
          val i = Intent(Intent.ACTION_VIEW, "imdb:///name/${ids.imdb.id}".toUri())
          try {
            startActivity(i)
          } catch (e: ActivityNotFoundException) {
            // IMDb App not installed.
            // Start in web browser
            openWebUrl("https://www.imdb.com/name/${ids.imdb.id}")
          }
        }
      }
    }
  }
}
