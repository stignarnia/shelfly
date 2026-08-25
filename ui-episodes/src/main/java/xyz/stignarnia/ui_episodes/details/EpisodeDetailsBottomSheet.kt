package xyz.stignarnia.ui_episodes.details

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.tabs.TabLayout
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.Config.IMAGE_FADE_DURATION_MS
import xyz.stignarnia.common.Config.SPOILERS_HIDE_SYMBOL
import xyz.stignarnia.common.Config.SPOILERS_REGEX
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.common.sheets.ratings.RatingsBottomSheet
import xyz.stignarnia.ui_base.common.sheets.ratings.RatingsBottomSheet.Options.Operation
import xyz.stignarnia.ui_base.common.sheets.ratings.RatingsBottomSheet.Options.Type
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.capitalizeWords
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.gone
import xyz.stignarnia.ui_base.utilities.extensions.invisible
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.optionalParcelable
import xyz.stignarnia.ui_base.utilities.extensions.requireParcelable
import xyz.stignarnia.ui_base.utilities.extensions.setTextFade
import xyz.stignarnia.ui_base.utilities.extensions.showErrorSnackbar
import xyz.stignarnia.ui_base.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.ui_base.utilities.extensions.visible
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.extensions.withFailListener
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_episodes.R
import xyz.stignarnia.ui_episodes.databinding.ViewEpisodeDetailsBinding
import xyz.stignarnia.ui_episodes.details.links.EpisodeLinksBottomSheet
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_navigation.java.NavigationArgs
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ACTION_EPISODE_TAB_SELECTED
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_OPTIONS
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_EPISODE_DETAILS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

@AndroidEntryPoint
class EpisodeDetailsBottomSheet : BaseBottomSheetFragment(R.layout.view_episode_details) {

  companion object {
    fun createBundle(
      showIds: Ids,
      episode: Episode,
      seasonEpisodesIds: List<Int>?,
      isWatched: Boolean,
      showTabs: Boolean,
    ): Bundle {
      val options = Options(
        showIds = showIds,
        episode = episode,
        seasonEpisodesIds = seasonEpisodesIds,
        isWatched = isWatched,
        showTabs = showTabs,
      )
      return Bundle().apply { putParcelable(ARG_OPTIONS, options) }
    }
  }

  private val viewModel by viewModels<EpisodeDetailsViewModel>()
  private val binding by viewBinding(ViewEpisodeDetailsBinding::bind)

  private val options by lazy { requireParcelable<Options>(ARG_OPTIONS) }
  private val cornerRadius by lazy { dimenToPx(R.dimen.bottomSheetCorner).toFloat() }

  private var spoilerTitle: String? = null
  private var spoilerDescription: String? = null
  private var spoilerRating: String? = null

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()

    with(viewModel) {
      launchAndRepeatStarted(
        { uiState.collect { render(it) } },
        { messageFlow.collect { renderSnackbar(it) } },
        doAfterLaunch = {
          val (ids, episode, seasonEpisodes) = options
          loadLastWatchedAt(ids.tmdb, episode)
          loadSeason(ids.tmdb, episode, seasonEpisodes?.toIntArray())
          loadImage(ids.tmdb, episode)
          loadTranslation(ids.tmdb, episode)
          loadRatings(ids.tmdb, episode)
        },
      )
    }
  }

  private fun setupView() {
    binding.run {
      val (showIds, episode, _, isWatched, showTabs) = options
      episodeDetailsTitle.text = when (episode.title) {
        "Episode ${episode.number}" -> String.format(
          ENGLISH,
          requireContext().getString(R.string.textEpisode),
          episode.number,
        )
        else -> episode.title
      }
      episodeDetailsOverview.text = episode.overview.ifBlank { getString(R.string.textNoDescription) }
      episodeDetailsRating.visibleIf(episode.votes > 0)
      episodeDetailsWatchedAt.visibleIf(episode.lastWatchedAt != null || isWatched)
      if (!showTabs) episodeDetailsTabs.gone()
      episodeDetailsRating.text = resources.getQuantityString(
        R.plurals.textVotes,
        episode.votes,
        episode.rating,
        episode.votes,
      )
      episodeDetailsLinksButton.onClick { openLinksSheet() }
    }
  }

  private fun render(uiState: EpisodeDetailsUiState) {
    uiState.run {
      with(binding) {
        val episode = options.episode
        dateFormat?.let {
          val millis = episode.firstAired?.toInstant()?.toEpochMilli() ?: -1
          val date = if (millis == -1L) {
            getString(R.string.textTba)
          } else {
            it.format(dateFromMillis(millis).toLocalZone()).capitalizeWords()
          }
          val name = String.format(
            ENGLISH,
            getString(R.string.textSeasonEpisodeDate),
            episode.season,
            episode.number,
            date,
          )
          val runtime = "${episode.runtime} ${getString(R.string.textMinutesShort)}"
          episodeDetailsName.text = if (episode.runtime > 0) "$name | $runtime" else name
        }
        isImageLoading.let { episodeDetailsProgress.visibleIf(it) }
        image?.let { renderImage(it, spoilers) }
        episodes?.let { renderEpisodes(it) }
        rating?.let { state ->
          episodeDetailsRateProgress.visibleIf(state.rateLoading == true)
          episodeDetailsRateButton.visibleIf(state.rateLoading == false, gone = false)
          episodeDetailsRateButton.isEnabled = state.rateLoading == false
          episodeDetailsRateButton.onClick { openRateDialog() }
          if (state.hasRating()) {
            episodeDetailsRateButton.text = getString(R.string.textRatingOutOfTen, state.userRating?.rating.toString())
          } else {
            episodeDetailsRateButton.setText(R.string.textRate)
          }
        }
        spoilers?.let { renderRating(it) }
        renderTitle(translation, spoilers)
        renderDescription(translation, spoilers)
        renderWatchedAt(lastWatchedAt, dateFormat)
      }
    }
  }

  private fun renderTitle(
    translation: Translation?,
    spoilersSettings: SpoilersSettings?,
  ) {
    with(binding) {
      var title =
        if (translation?.title?.isNotBlank() == true) {
          translation.title
        } else if (episodeDetailsTitle.text.isBlank()) {
          when (options.episode.title) {
            "Episode ${options.episode.number}" -> {
              String.format(ENGLISH, requireContext().getString(R.string.textEpisode), options.episode.number)
            }
            else -> {
              options.episode.title
            }
          }
        } else {
          episodeDetailsTitle.text.toString()
        }

      val isEpisodeTitleHidden = !options.isWatched && spoilersSettings?.isEpisodeTitleHidden == true
      if (isEpisodeTitleHidden) {
        if (title.any { it.isLetter() }) {
          spoilerTitle = String(title.toCharArray())
        }
        title = SPOILERS_REGEX.replace(title, SPOILERS_HIDE_SYMBOL)
      }

      if (title.isNotBlank()) {
        episodeDetailsTitle.setTextFade(title, duration = 0)
      }

      if (spoilersSettings?.isTapToReveal == true) {
        episodeDetailsTitle.onClick {
          spoilerTitle?.let {
            episodeDetailsTitle.setTextFade(it, duration = 0)
          }
        }
      }
    }
  }

  private fun renderDescription(
    translation: Translation?,
    spoilersSettings: SpoilersSettings?,
  ) {
    with(binding) {
      var description =
        if (translation?.overview?.isNotBlank() == true) {
          translation.overview
        } else if (episodeDetailsOverview.text.isBlank()) {
          options.episode.overview.ifBlank {
            getString(R.string.textNoDescription)
          }
        } else {
          episodeDetailsOverview.text.toString()
        }

      if (!options.isWatched && spoilersSettings?.isEpisodeDescriptionHidden == true) {
        if (description.any { it.isLetter() }) {
          spoilerDescription = String(description.toCharArray())
        }
        description = SPOILERS_REGEX.replace(description, SPOILERS_HIDE_SYMBOL)
      }

      if (description.isNotBlank()) {
        episodeDetailsOverview.setTextFade(description, duration = 0)
      }

      if (spoilersSettings?.isTapToReveal == true) {
        episodeDetailsOverview.onClick {
          spoilerDescription?.let {
            episodeDetailsOverview.setTextFade(it, duration = 0)
          }
        }
      }
    }
  }

  private fun renderWatchedAt(
    watchedAt: ZonedDateTime?,
    dateFormat: DateTimeFormatter?,
  ) {
    with(binding) {
      dateFormat?.let {
        episodeDetailsWatchedAt.visibleIf(watchedAt != null)
        episodeDetailsWatchedAt.text = watchedAt?.toLocalZone()?.format(it)
      }
    }
  }

  private fun renderRating(spoilersSettings: SpoilersSettings) {
    with(binding) {
      val isSpoilerHidden = !options.isWatched && spoilersSettings.isEpisodeRatingHidden
      if (isSpoilerHidden) {
        if (spoilerRating == null) {
          spoilerRating = episodeDetailsRating.text.toString()
        }
        episodeDetailsRating.text = Config.SPOILERS_RATINGS_VOTES_HIDE_SYMBOL
      }

      if (spoilersSettings.isTapToReveal) {
        episodeDetailsRating.onClick {
          spoilerRating?.let {
            episodeDetailsRating.text = it
          }
        }
      }
    }
  }

  private fun renderImage(
    image: Image,
    spoilers: SpoilersSettings?,
    tapToReveal: Boolean = false,
  ) {
    with(binding) {
      if (!options.isWatched && spoilers?.isEpisodeImageHidden == true && !tapToReveal) {
        episodeDetailsImage.invisible()
        episodeDetailsImagePlaceholder.visible()
        episodeDetailsImagePlaceholder.setImageResource(R.drawable.ic_eye_no)
        if (spoilers.isTapToReveal) {
          episodeDetailsImagePlaceholder.onClick {
            renderImage(image, spoilers, tapToReveal = true)
          }
        }
        return
      }
      episodeDetailsImage.visible()
      episodeDetailsImagePlaceholder.invisible()
      Glide
        .with(this@EpisodeDetailsBottomSheet)
        .load("${Config.TMDB_IMAGE_BASE_STILL_URL}${image.fileUrl}")
        .transform(CenterCrop(), GranularRoundedCorners(cornerRadius, cornerRadius, 0F, 0F))
        .transition(DrawableTransitionOptions.withCrossFade(IMAGE_FADE_DURATION_MS))
        .withFailListener {
          episodeDetailsImagePlaceholder.visible()
          episodeDetailsImagePlaceholder.setImageResource(R.drawable.ic_television)
          episodeDetailsImagePlaceholder.setOnClickListener(null)
        }.into(episodeDetailsImage)
    }
  }

  private fun renderEpisodes(episodes: List<Episode>) {
    with(binding.episodeDetailsTabs) {
      removeAllTabs()
      removeOnTabSelectedListener(tabSelectedListener)
      episodes.forEach {
        addTab(
          newTab()
            .setText("${options.episode.season}x${it.number.toString().padStart(2, '0')}")
            .setTag(it),
        )
      }
      val index = episodes.indexOfFirst { it.number == options.episode.number }
      // Small trick to avoid UI tab change flick
      getTabAt(index)?.select()
      post {
        getTabAt(index)?.select()
        addOnTabSelectedListener(tabSelectedListener)
      }
      if (options.showTabs && episodes.isNotEmpty()) {
        fadeIn(duration = 200, startDelay = 100, withHardware = true)
      } else {
        gone()
      }
    }
  }

  private fun renderSnackbar(message: MessageEvent) {
    when (message) {
      is MessageEvent.Info -> binding.episodeDetailsSnackbarHost.showInfoSnackbar(getString(message.textRestId))
      is MessageEvent.Error -> binding.episodeDetailsSnackbarHost.showErrorSnackbar(getString(message.textRestId))
    }
  }

  private fun openRateDialog() {
    setFragmentResultListener(NavigationArgs.REQUEST_RATING) { _, bundle ->
      when (bundle.optionalParcelable<Operation>(NavigationArgs.RESULT)) {
        Operation.SAVE -> renderSnackbar(MessageEvent.Info(R.string.textRateSaved))
        Operation.REMOVE -> renderSnackbar(MessageEvent.Info(R.string.textRateRemoved))
        else -> Timber.w("Unknown result.")
      }
      viewModel.loadRatings(options.showIds.tmdb, options.episode)
      setFragmentResult(
        REQUEST_EPISODE_DETAILS,
        Bundle().apply { putBoolean(NavigationArgs.ACTION_RATING_CHANGED, true) },
      )
    }
    val bundle = RatingsBottomSheet.createBundle(
      id = options.episode.ids.tmdb,
      type = Type.EPISODE,
      showId = options.showIds.tmdb,
      seasonNumber = options.episode.season,
      episodeNumber = options.episode.number,
    )
    navigateTo(R.id.actionEpisodeDetailsDialogToRate, bundle)
  }

  private fun openLinksSheet() {
    val bundle = EpisodeLinksBottomSheet.createBundle(
      showIds = options.showIds,
      episode = options.episode,
    )
    navigateTo(R.id.actionEpisodeDetailsDialogToLink, bundle)
  }

  private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
    override fun onTabSelected(tab: TabLayout.Tab?) {
      binding.episodeDetailsTabs.removeOnTabSelectedListener(this)
      closeSheet()
      setFragmentResult(
        REQUEST_EPISODE_DETAILS,
        Bundle().apply {
          (tab?.tag as? Episode)?.let { putParcelable(ACTION_EPISODE_TAB_SELECTED, it) }
        },
      )
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

    override fun onTabReselected(tab: TabLayout.Tab?) = Unit
  }

  @Parcelize
  private data class Options(
    val showIds: Ids,
    val episode: Episode,
    val seasonEpisodesIds: List<Int>?,
    val isWatched: Boolean,
    val showTabs: Boolean,
  ) : Parcelable
}
