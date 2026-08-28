package xyz.stignarnia.uiShow

import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.DecelerateInterpolator
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import xyz.stignarnia.common.Config.IMAGE_FADE_DURATION_MS
import xyz.stignarnia.common.Config.SPOILERS_HIDE_SYMBOL
import xyz.stignarnia.common.Config.SPOILERS_REGEX
import xyz.stignarnia.common.Mode
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.sheets.links.LinksBottomSheet
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet.Options.Operation.REMOVE
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet.Options.Operation.SAVE
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet.Options.Type
import xyz.stignarnia.uiBase.utilities.SnackbarHost
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.capitalizeWords
import xyz.stignarnia.uiBase.utilities.extensions.copyToClipboard
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateBack
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.onLongClick
import xyz.stignarnia.uiBase.utilities.extensions.openWebUrl
import xyz.stignarnia.uiBase.utilities.extensions.optionalParcelable
import xyz.stignarnia.uiBase.utilities.extensions.requireLong
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.extensions.screenHeight
import xyz.stignarnia.uiBase.utilities.extensions.screenWidth
import xyz.stignarnia.uiBase.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.extensions.withFailListener
import xyz.stignarnia.uiBase.utilities.extensions.withSuccessListener
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageFamily.SHOW
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE
import xyz.stignarnia.uiModel.ImageType.FANART
import xyz.stignarnia.uiModel.RatingState
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Tip.SHOW_DETAILS_GALLERY
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_FAMILY
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_TYPE
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_MANAGE_LISTS
import xyz.stignarnia.uiShow.ShowDetailsEvent.Finish
import xyz.stignarnia.uiShow.databinding.FragmentShowDetailsBinding
import xyz.stignarnia.uiShow.views.AddToShowsButton
import java.util.Locale.ENGLISH

@AndroidEntryPoint
class ShowDetailsFragment : BaseFragment<ShowDetailsViewModel>(R.layout.fragment_show_details) {
  override val navigationId = R.id.showDetailsFragment
  val binding by viewBinding(FragmentShowDetailsBinding::bind)

  override val viewModel by viewModels<ShowDetailsViewModel>()

  private val showId by lazy { IdTmdb(requireLong(ARG_SHOW_ID)) }

  private val imageHeight by lazy {
    if (resources.configuration.orientation == ORIENTATION_PORTRAIT) {
      screenHeight()
    } else {
      screenWidth()
    }
  }
  private val imageRatio by lazy { resources.getString(R.string.detailsImageRatio).toFloat() }
  private val imagePadded by lazy { resources.getBoolean(R.bool.detailsImagePadded) }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupInsets()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
      { viewModel.messageFlow.collect { renderSnack(it) } },
      doAfterLaunch = {
        if (!isInitialized) {
          viewModel.loadDetails(showId)
          isInitialized = true
        }
      },
    )
  }

  private fun setupView() {
    with(binding) {
      hideNavigation()
      showDetailsImageGuideline.setGuidelineBegin((imageHeight * imageRatio).toInt())
      showDetailsBackArrow.onClick { navigateBack() }
      showDetailsImage.onClick {
        val bundle =
          Bundle().apply {
            putLong(ARG_SHOW_ID, showId.id)
            putSerializable(ARG_FAMILY, SHOW)
            putSerializable(ARG_TYPE, FANART)
          }
        navigateToSafe(R.id.actionShowDetailsFragmentToArtGallery, bundle)
      }
      showDetailsTipGallery.onClick {
        it.gone()
        showTip(SHOW_DETAILS_GALLERY)
      }
      showDetailsAddButton.run {
        isEnabled = false
        onAddMyShowsClickListener = { viewModel.addFollowedShow() }
        onAddWatchlistClickListener = { viewModel.addWatchlistShow() }
        onRemoveClickListener = { viewModel.removeFromFollowed() }
      }
      showDetailsManageListsLabel.onClick { openListsDialog() }
      showDetailsHideLabel.onClick { viewModel.addHiddenShow() }
      showDetailsTitle.onClick {
        requireContext().copyToClipboard(showDetailsTitle.text.toString())
        showSnack(MessageEvent.Info(R.string.textCopiedToClipboard))
      }
      showDetailsDescription.onLongClick {
        val text = showDetailsDescription.text.toString()
        if (text.count { it.toString() == SPOILERS_HIDE_SYMBOL } == 0) {
          requireContext().copyToClipboard(text)
          showSnack(MessageEvent.Info(R.string.textCopiedToClipboard))
        }
      }
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { _, insets, _, _ ->
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        if (imagePadded) {
          showDetailsMainLayout
            .updatePadding(top = inset.top)
        }
        showDetailsMainContent.updatePadding(bottom = inset.bottom + dimenToPx(R.dimen.spaceNormal))
        (showDetailsBackArrow.layoutParams as MarginLayoutParams).updateMargins(top = inset.top)
      }
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is Finish -> navigateBack()
    }
  }

  private fun render(uiState: ShowDetailsUiState) {
    uiState.run {
      with(binding) {
        show?.let { show ->
          showDetailsStatus.text = getString(show.status.displayName)
          renderTitleDescription(show, translation, followedState, spoilers)
          renderExtraInfo(show)
          showDetailsActions.trailerChip.run {
            isEnabled = show.trailer.isNotBlank()
            alpha = if (isEnabled) 1.0F else 0.35F
            onClick {
              openWebUrl(show.trailer) ?: showSnack(MessageEvent.Info(R.string.errorCouldNotFindApp))
            }
          }
          showDetailsActions.linksChip.onClick {
            val args = LinksBottomSheet.createBundle(show)
            navigateToSafe(R.id.actionShowDetailsFragmentToLinks, args)
          }
          showDetailsActions.shareChip.run {
            isEnabled =
              show.ids.imdb.id
                .isNotBlank()
            alpha = if (isEnabled) 1.0F else 0.35F
            onClick { openShareSheet(show) }
          }
          showDetailsAddButton.isEnabled = true
        }
        showLoading?.let {
          showDetailsMainLayout.fadeIf(!it, hardware = true)
          showDetailsMainProgress.visibleIf(it)
        }
        followedState?.let {
          when {
            it.isMyShows -> showDetailsAddButton.setState(AddToShowsButton.State.IN_MY_SHOWS, it.withAnimation)
            it.isWatchlist -> showDetailsAddButton.setState(AddToShowsButton.State.IN_WATCHLIST, it.withAnimation)
            it.isHidden -> showDetailsAddButton.setState(AddToShowsButton.State.IN_HIDDEN, it.withAnimation)
            else -> showDetailsAddButton.setState(AddToShowsButton.State.ADD, it.withAnimation)
          }
          showDetailsHideLabel.visibleIf(!it.isHidden)
        }
        listsCount?.let {
          val text =
            if (it > 0) {
              getString(R.string.textShowManageListsCount, it)
            } else {
              getString(R.string.textShowManageLists)
            }
          showDetailsManageListsLabel.text = text
        }
        image?.let { renderImage(it) }
        ratingState?.let { renderRating(it) }
      }
    }
  }

  private fun renderTitleDescription(
    show: Show,
    translation: Translation?,
    followedState: ShowDetailsUiState.FollowedState?,
    spoilersSettings: SpoilersSettings?,
  ) {
    with(binding) {
      var title = show.title
      var description = show.overview

      if (translation?.title?.isNotBlank() == true) {
        title = translation.title
      }
      if (translation?.overview?.isNotBlank() == true) {
        description = translation.overview
      }

      if (followedState == null || spoilersSettings == null) {
        showDetailsTitle.text = title
        showDetailsDescription.text = description
        return
      }

      val isMyShowHidden = spoilersSettings.isMyShowsHidden && followedState.isMyShows
      val isWatchlistHidden = spoilersSettings.isWatchlistShowsHidden && followedState.isWatchlist
      val isHiddenShowHidden = spoilersSettings.isHiddenShowsHidden && followedState.isHidden
      val isNotCollectedHidden = spoilersSettings.isNotCollectedShowsHidden && (!followedState.isInCollection())

      if (isMyShowHidden || isWatchlistHidden || isHiddenShowHidden || isNotCollectedHidden) {
        showDetailsDescription.tag = description
        description = SPOILERS_REGEX.replace(description, SPOILERS_HIDE_SYMBOL)

        if (spoilersSettings.isTapToReveal) {
          with(showDetailsDescription) {
            onClick {
              tag?.let { text = it.toString() }
              enableFoldOnClick()
            }
          }
        }
      }

      showDetailsTitle.text = title
      showDetailsDescription.text = description
    }
  }

  private fun renderExtraInfo(show: Show) {
    val year = if (show.year > 0) String.format(ENGLISH, "%d", show.year) else ""
    val country = if (show.country.isNotBlank()) "(${show.country})" else ""
    val genres =
      show.genres
        .take(5)
        .mapNotNull { Genre.fromSlug(it) }
        .joinToString(", ") { getString(it.displayName) }

    var extraInfoText =
      getString(
        R.string.textShowExtraInfo,
        show.network,
        year,
        country.uppercase(),
        "⏲ ${show.runtime}",
        getString(R.string.textMinutesShort),
        genres,
      )

    if (genres.isEmpty()) {
      extraInfoText = extraInfoText.trim().removeSuffix("|")
    }

    binding.showDetailsExtraInfo.text = extraInfoText
  }

  private fun renderRating(rating: RatingState) {
    with(binding.showDetailsActions.rateChip) {
      isEnabled = rating.rateLoading == false
      alpha = if (isEnabled) 1.0F else 0.35F

      text =
        if (rating.hasRating()) {
          "${rating.userRating?.rating} / 10"
        } else {
          getString(R.string.textRate)
        }

      onClick {
        openRateDialog()
      }
    }
  }

  private fun renderImage(image: Image) {
    with(binding) {
      if (image.status == UNAVAILABLE) {
        showDetailsImageProgress.gone()
        showDetailsPlaceholder.visible()
        showDetailsImage.isClickable = false
        showDetailsImage.isEnabled = false
        return
      }
      Glide
        .with(this@ShowDetailsFragment)
        .load(image.fullFileUrl)
        .transform(CenterCrop())
        .transition(withCrossFade(IMAGE_FADE_DURATION_MS))
        .withFailListener {
          showDetailsImageProgress.gone()
          showDetailsPlaceholder.visible()
          showDetailsImage.isClickable = true
          showDetailsImage.isEnabled = true
        }.withSuccessListener {
          showDetailsImageProgress.gone()
          showDetailsPlaceholder.gone()
          showDetailsTipGallery.fadeIf(!isTipShown(SHOW_DETAILS_GALLERY))
        }.into(showDetailsImage)
    }
  }

  private fun renderSnack(event: MessageEvent) {
    if (event.textResId == R.string.errorMalformedShow) {
      val host = (requireActivity() as SnackbarHost).provideSnackbarLayout()
      val snack =
        host.showInfoSnackbar(getString(event.textResId), length = Snackbar.LENGTH_INDEFINITE) {
          viewModel.removeMalformedShow(showId)
        }
      snackbars.add(snack)
      return
    }
    showSnack(event)
  }

  private fun openShareSheet(show: Show) {
    val intent =
      Intent().apply {
        val text =
          "${show.title}:" +
            "\n" +
            "https://www.imdb.com/title/${show.ids.imdb.id}"
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
      }

    val shareIntent = Intent.createChooser(intent, "Share ${show.title}")
    startActivity(shareIntent)
  }

  private fun openRateDialog() {
    setFragmentResultListener(NavigationArgs.REQUEST_RATING) { _, bundle ->
      when (bundle.optionalParcelable<RatingsBottomSheet.Options.Operation>(NavigationArgs.RESULT)) {
        SAVE -> renderSnack(MessageEvent.Info(R.string.textRateSaved))
        REMOVE -> renderSnack(MessageEvent.Info(R.string.textRateRemoved))
        else -> Timber.w("Unknown result")
      }
      viewModel.loadUserRating()
    }
    val bundle = RatingsBottomSheet.createBundle(showId, Type.SHOW)
    navigateToSafe(R.id.actionShowDetailsFragmentToRating, bundle)
  }

  private fun openListsDialog() {
    if (findNavControl()?.currentDestination?.id != R.id.showDetailsFragment) {
      return
    }
    setFragmentResultListener(REQUEST_MANAGE_LISTS) { _, _ -> viewModel.loadListsCount() }
    val bundle =
      Bundle().apply {
        putLong(ARG_ID, showId.id)
        putSerializable(ARG_TYPE, Mode.SHOWS.type)
      }
    navigateToSafe(R.id.actionShowDetailsFragmentToManageLists, bundle)
  }

  fun showStreamingsView(animate: Boolean) {
    with(binding) {
      if (!animate) {
        showDetailsStreamingsFragment.visible()
        return
      }
      val animation =
        ConstraintSet().apply {
          clone(showDetailsMainContent)
          setVisibility(showDetailsStreamingsFragment.id, View.VISIBLE)
        }
      val transition =
        AutoTransition().apply {
          interpolator = DecelerateInterpolator(1.5F)
          duration = 200
        }
      TransitionManager.beginDelayedTransition(showDetailsMainContent, transition)
      animation.applyTo(showDetailsMainContent)
    }
  }

  override fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      isEnabled = false
      findNavControl()?.popBackStack()
    }
  }
}
