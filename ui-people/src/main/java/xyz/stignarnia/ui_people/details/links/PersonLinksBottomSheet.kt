package xyz.stignarnia.ui_people.details.links

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.os.bundleOf
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.openWebUrl
import xyz.stignarnia.ui_base.utilities.extensions.requireParcelable
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Person
import xyz.stignarnia.ui_navigation.java.NavigationArgs
import xyz.stignarnia.ui_people.R
import xyz.stignarnia.ui_people.databinding.ViewPersonLinksBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

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
      return bundleOf(NavigationArgs.ARG_OPTIONS to options)
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
          val i = Intent(Intent.ACTION_VIEW)
          i.data = Uri.parse("imdb:///name/${ids.imdb.id}")
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
