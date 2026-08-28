package xyz.stignarnia.uiShow.episodes

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import xyz.stignarnia.common.Config
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Companion.REQUEST_DATE_SELECTION
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Companion.RESULT_DATE_SELECTION
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Result
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet.Options.Operation
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet.Options.Type
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.optionalParcelable
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiEpisodes.details.EpisodeDetailsBottomSheet
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiShow.R
import xyz.stignarnia.uiShow.databinding.FragmentShowDetailsEpisodesBinding
import xyz.stignarnia.uiShow.episodes.ShowDetailsEpisodesEvent.Finish
import xyz.stignarnia.uiShow.episodes.ShowDetailsEpisodesEvent.OpenEpisodeDateSelection
import xyz.stignarnia.uiShow.episodes.ShowDetailsEpisodesEvent.OpenEpisodeDetails
import xyz.stignarnia.uiShow.episodes.ShowDetailsEpisodesEvent.OpenRateSeason
import xyz.stignarnia.uiShow.episodes.ShowDetailsEpisodesEvent.OpenSeasonDateSelection
import xyz.stignarnia.uiShow.episodes.ShowDetailsEpisodesEvent.RequestWidgetsUpdate
import xyz.stignarnia.uiShow.episodes.recycler.EpisodesAdapter
import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonListItem
import java.util.Locale

@AndroidEntryPoint
class ShowDetailsEpisodesFragment :
  BaseFragment<ShowDetailsEpisodesViewModel>(
    R.layout.fragment_show_details_episodes,
  ) {
  companion object {
    fun createBundle(
      showId: IdTmdb,
      seasonId: IdTmdb,
    ): Bundle =
      Bundle().apply {
        putParcelable(NavigationArgs.ARG_OPTIONS, Options(showId, seasonId))
      }
  }

  override val navigationId = R.id.showDetailsEpisodesFragment
  private val binding by viewBinding(FragmentShowDetailsEpisodesBinding::bind)

  override val viewModel by viewModels<ShowDetailsEpisodesViewModel>()

  private var episodesAdapter: EpisodesAdapter? = null
  private var isLocked = true

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)

    setupView()
    setupInsets()
    setupRecycler()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it as ShowDetailsEpisodesEvent<*>) } },
    )
  }

  override fun onResume() {
    super.onResume()
    viewModel.launchRefreshWatchedEpisodes()
  }

  private fun setupView() {
    with(binding) {
      episodesBackArrow.onClick { findNavControl()?.popBackStack() }
      episodesTitle.onClick { findNavControl()?.popBackStack() }
      episodesUnlockButton.onClick(safe = false) { toggleEpisodesLock() }
      listOf(episodesSeasonRateButton, episodesSeasonMyStarIcon).onClick {
        viewModel.openRateSeasonDialog()
      }
    }
  }

  private fun setupInsets() {
    with(binding) {
      episodesRoot.doOnApplyWindowInsets { view, insets, padding, _ ->
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(top = padding.top + inset.top)
        episodesRecycler.updatePadding(bottom = episodesRecycler.paddingBottom + inset.bottom)
      }
    }
  }

  private fun setupRecycler() {
    episodesAdapter =
      EpisodesAdapter(
        itemClickListener = { episode: Episode, isWatched: Boolean ->
          viewModel.openEpisodeDetails(episode, isWatched)
        },
        itemCheckedListener = { episode: Episode, isChecked: Boolean ->
          viewModel.onEpisodeCheck(episode, isChecked)
        },
      )
    binding.episodesRecycler.apply {
      setHasFixedSize(true)
      adapter = episodesAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
      itemAnimator = null
    }
  }

  private fun render(uiState: ShowDetailsEpisodesUiState) {
    with(uiState) {
      with(binding) {
        season?.let {
          episodesTitle.text =
            if (it.season.isSpecial()) {
              getString(R.string.textSpecials)
            } else {
              String.format(Locale.ENGLISH, getString(R.string.textSeason), it.season.number)
            }
          episodesOverview.text = it.season.overview
          episodesOverview.visibleIf(it.season.overview.isNotBlank())
          episodesCheckbox.run {
            isEnabled = it.episodes.all { ep -> ep.episode.hasAired(it.season) } || !isLocked
            isChecked = it.isWatched
            setOnClickListener {
              viewModel.onSeasonChecked(season, isChecked)
              if (isChecked) {
                isChecked = false
              }
            }
          }
          episodesUnlockButton.visibleIf(
            !it.season.isSpecial() &&
              it.episodes.any { ep ->
                !ep.episode.hasAired(it.season)
              },
          )

          renderSeasonRating(season)
        }
        episodes?.let {
          episodesAdapter?.setItems(it)
          if (isInitialLoad == true) {
            episodesRecycler.scheduleLayoutAnimation()
          }
        }
      }
    }
  }

  private fun renderSeasonRating(season: SeasonListItem) {
    with(binding) {
      val seasonRating = season.season.rating
      episodesStarIcon.visibleIf(seasonRating > 0F)
      episodesSeasonRating.visibleIf(seasonRating > 0F)

      val seasonRatingString = String.format(Locale.ENGLISH, "%.1f", seasonRating)
      if (!season.isWatched && season.isRatingHidden) {
        episodesSeasonRating.tag = seasonRatingString
        episodesSeasonRating.text = Config.SPOILERS_RATINGS_HIDE_SYMBOL
        if (season.isRatingTapToReveal) {
          with(episodesSeasonRating) {
            onClick {
              tag?.let { text = it.toString() }
              isClickable = false
            }
          }
        }
      } else {
        episodesSeasonRating.text = seasonRatingString
      }

      val ratingState = season.userRating
      episodesSeasonRateButton.visibleIf(ratingState.userRating == null)
      episodesSeasonMyStarIcon.visibleIf(ratingState.userRating != null)
      episodesSeasonMyRating.visibleIf(ratingState.userRating != null)
      ratingState.userRating?.let {
        episodesSeasonMyStarIcon.isEnabled = true
        episodesSeasonMyRating.text = String.format(Locale.ENGLISH, "%d", it.rating)
      }
    }
  }

  private fun handleEvent(event: ShowDetailsEpisodesEvent<*>) {
    when (event) {
      is OpenEpisodeDetails -> openEpisodeDetails(event.bundle, event.isWatched)
      is OpenRateSeason -> openRateSeasonDialog(event.showId, event.season)
      is OpenEpisodeDateSelection -> openDateSelectionDialog(event.episode)
      is OpenSeasonDateSelection -> openDateSelectionDialog(event.season)
      is RequestWidgetsUpdate -> (requireAppContext() as WidgetsProvider).requestShowsWidgetsUpdate()
      is Finish -> findNavControl()?.popBackStack()
    }
  }

  private fun toggleEpisodesLock() {
    isLocked = !isLocked

    with(binding) {
      episodesUnlockButton.setImageResource(if (isLocked) R.drawable.ic_locked else R.drawable.ic_unlocked)
      episodesCheckbox.isEnabled = !isLocked
    }
    episodesAdapter?.toggleEpisodesLock()
  }

  private fun openEpisodeDetails(
    episodeBundle: EpisodeBundle,
    isWatched: Boolean,
  ) {
    val (episode, season, show) = episodeBundle
    setFragmentResultListener(NavigationArgs.REQUEST_EPISODE_DETAILS) { _, bundle ->
      when {
        bundle.containsKey(NavigationArgs.ACTION_EPISODE_TAB_SELECTED) -> {
          val selectedEpisode = bundle.requireParcelable<Episode>(NavigationArgs.ACTION_EPISODE_TAB_SELECTED)
          viewModel.openEpisodeDetails(selectedEpisode)
        }

        bundle.containsKey(NavigationArgs.ACTION_RATING_CHANGED) -> {
          viewModel.loadEpisodesRating()
        }
      }
    }

    val bundle =
      EpisodeDetailsBottomSheet.createBundle(
        showIds = show.ids,
        episode = episode,
        seasonEpisodesIds = season.episodes.map { it.number },
        isWatched = isWatched,
        showTabs = true,
      )
    navigateToSafe(R.id.actionEpisodesFragmentToEpisodesDetails, bundle)
  }

  private fun openRateSeasonDialog(
    showId: IdTmdb,
    season: Season,
  ) {
    setFragmentResultListener(NavigationArgs.REQUEST_RATING) { _, bundle ->
      when (bundle.optionalParcelable<Operation>(NavigationArgs.RESULT)) {
        Operation.SAVE -> showSnack(MessageEvent.Info(R.string.textRateSaved))
        Operation.REMOVE -> showSnack(MessageEvent.Info(R.string.textRateRemoved))
        else -> Timber.w("Unknown result")
      }
      viewModel.loadSeasonRating()
    }

    val bundle =
      RatingsBottomSheet.createBundle(
        id = season.ids.tmdb,
        type = Type.SEASON,
        showId = showId,
        seasonNumber = season.number,
      )
    navigateToSafe(R.id.actionEpisodesFragmentToRating, bundle)
  }

  private fun openDateSelectionDialog(episode: Episode) {
    setFragmentResultListener(REQUEST_DATE_SELECTION) { _, bundle ->
      when (val result = bundle.requireParcelable<Result>(RESULT_DATE_SELECTION)) {
        is Result.Now -> viewModel.setEpisodeWatched(episode, true)
        is Result.CustomDate -> viewModel.setEpisodeWatched(episode, true, result.date)
        is Result.ReleaseDate -> viewModel.setEpisodeWatched(episode, true, result.date)
      }
    }
    val options = DateSelectionBottomSheet.createBundle(episode.firstAired)
    navigateToSafe(R.id.actionEpisodesFragmentToDateSelection, options)
  }

  private fun openDateSelectionDialog(season: SeasonListItem) {
    setFragmentResultListener(REQUEST_DATE_SELECTION) { _, bundle ->
      when (val result = bundle.requireParcelable<Result>(RESULT_DATE_SELECTION)) {
        is Result.Now -> viewModel.setSeasonWatched(season, true)
        is Result.CustomDate -> viewModel.setSeasonWatched(season, true, result.date)
        is Result.ReleaseDate -> viewModel.setSeasonWatched(season, true, result.date)
      }
    }
    val options = DateSelectionBottomSheet.createBundle(season.season.firstAired)
    navigateToSafe(R.id.actionEpisodesFragmentToDateSelection, options)
  }

  override fun onDestroyView() {
    episodesAdapter = null
    super.onDestroyView()
  }

  @Parcelize
  data class Options(
    val showId: IdTmdb,
    val seasonId: IdTmdb,
  ) : Parcelable
}
