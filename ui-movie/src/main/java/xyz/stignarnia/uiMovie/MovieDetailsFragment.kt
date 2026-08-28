package xyz.stignarnia.uiMovie

import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Result
import xyz.stignarnia.uiBase.common.sheets.links.LinksBottomSheet
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet
import xyz.stignarnia.uiBase.common.sheets.ratings.RatingsBottomSheet.Options.Operation
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
import xyz.stignarnia.uiModel.ImageFamily.MOVIE
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE
import xyz.stignarnia.uiModel.ImageType.FANART
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.RatingState
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiMovie.MovieDetailsEvent.Finish
import xyz.stignarnia.uiMovie.MovieDetailsEvent.OpenDateSelectionSheet
import xyz.stignarnia.uiMovie.MovieDetailsEvent.RequestWidgetsUpdate
import xyz.stignarnia.uiMovie.databinding.FragmentMovieDetailsBinding
import xyz.stignarnia.uiMovie.helpers.MovieDetailsMeta
import xyz.stignarnia.uiMovie.views.AddToMoviesButton.State.ADD
import xyz.stignarnia.uiMovie.views.AddToMoviesButton.State.IN_HIDDEN
import xyz.stignarnia.uiMovie.views.AddToMoviesButton.State.IN_MY_MOVIES
import xyz.stignarnia.uiMovie.views.AddToMoviesButton.State.IN_WATCHLIST
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_FAMILY
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_TYPE
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_MANAGE_LISTS
import java.time.ZoneOffset.UTC
import java.util.Locale.ENGLISH
import java.util.Locale.ROOT

@AndroidEntryPoint
class MovieDetailsFragment : BaseFragment<MovieDetailsViewModel>(R.layout.fragment_movie_details) {
  override val navigationId = R.id.movieDetailsFragment
  val binding by viewBinding(FragmentMovieDetailsBinding::bind)

  override val viewModel by viewModels<MovieDetailsViewModel>()

  private val movieId by lazy { IdTmdb(requireLong(ARG_MOVIE_ID)) }

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
      { viewModel.messageFlow.collect { renderSnack(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
      doAfterLaunch = {
        if (!isInitialized) {
          viewModel.loadDetails(movieId)
          isInitialized = true
        }
      },
    )
  }

  private fun setupView() {
    with(binding) {
      hideNavigation()
      movieDetailsImageGuideline.setGuidelineBegin((imageHeight * imageRatio).toInt())
      movieDetailsBackArrow.onClick { navigateBack() }
      movieDetailsImage.onClick {
        val bundle =
          Bundle().apply {
            putLong(ARG_MOVIE_ID, movieId.id)
            putSerializable(ARG_FAMILY, MOVIE)
            putSerializable(ARG_TYPE, FANART)
          }
        navigateToSafe(R.id.actionMovieDetailsFragmentToArtGallery, bundle)
      }
      movieDetailsAddButton.run {
        isEnabled = false
        onAddMyMoviesClickListener = { viewModel.addToMyMovies() }
        onAddWatchLaterClickListener = { viewModel.addToWatchlist() }
        onRemoveClickListener = { viewModel.removeFromMyMovies() }
      }
      movieDetailsManageListsLabel.onClick { openListsDialog() }
      movieDetailsHideLabel.onClick { viewModel.addToHidden() }
      movieDetailsTitle.onClick {
        requireContext().copyToClipboard(movieDetailsTitle.text.toString())
        showSnack(MessageEvent.Info(R.string.textCopiedToClipboard))
      }
      movieDetailsDescription.onLongClick {
        requireContext().copyToClipboard(movieDetailsDescription.text.toString())
        showSnack(MessageEvent.Info(R.string.textCopiedToClipboard))
      }
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { view, insets, _, _ ->
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        if (imagePadded) {
          movieDetailsMainLayout
            .updatePadding(top = inset.top)
        }
        movieDetailsMainContent.updatePadding(bottom = inset.bottom + dimenToPx(R.dimen.spaceNormal))
        (movieDetailsBackArrow.layoutParams as ViewGroup.MarginLayoutParams).updateMargins(top = inset.top)
      }
    }
  }

  private fun render(uiState: MovieDetailsUiState) {
    uiState.run {
      with(binding) {
        movie?.let { movie ->
          renderTitleDescription(movie, translation, followedState, spoilers)
          renderExtraInfo(movie, meta)
          movieDetailsStatus.text = getString(movie.status.displayName)
          movieDetailsActions.trailerChip.run {
            isEnabled = movie.trailer.isNotBlank()
            alpha = if (isEnabled) 1.0F else 0.35F
            onClick {
              openWebUrl(movie.trailer) ?: showSnack(MessageEvent.Info(R.string.errorCouldNotFindApp))
            }
          }
          movieDetailsActions.linksChip.run {
            onClick {
              val args = LinksBottomSheet.createBundle(movie)
              navigateToSafe(R.id.actionMovieDetailsFragmentToLinks, args)
            }
          }
          movieDetailsActions.shareChip.run {
            isEnabled =
              movie.ids.imdb.id
                .isNotBlank()
            alpha = if (isEnabled) 1.0F else 0.35F
            onClick { openShareSheet(movie) }
          }
          movieDetailsAddButton.isEnabled = true
        }
        movieLoading?.let {
          movieDetailsMainLayout.fadeIf(!it, hardware = true)
          movieDetailsMainProgress.visibleIf(it)
        }
        followedState?.let {
          when {
            it.isMyMovie -> movieDetailsAddButton.setState(IN_MY_MOVIES, it.withAnimation)
            it.isWatchlist -> movieDetailsAddButton.setState(IN_WATCHLIST, it.withAnimation)
            it.isHidden -> movieDetailsAddButton.setState(IN_HIDDEN, it.withAnimation)
            else -> movieDetailsAddButton.setState(ADD, it.withAnimation)
          }
          movieDetailsHideLabel.visibleIf(!it.isHidden)
          movieDetailsWatchedBadge.visibleIf(it.isMyMovie && it.watchedAt != null)
          it.watchedAt?.let { date ->
            movieDetailsWatchedBadge.text = uiState.meta?.watchedAtDateFormat?.format(date.toLocalZone())
          }
        }
        image?.let { renderImage(it) }
        listsCount?.let {
          val text =
            if (it > 0) {
              getString(R.string.textMovieManageListsCount, it)
            } else {
              getString(R.string.textMovieManageLists)
            }
          movieDetailsManageListsLabel.text = text
        }
        ratingState?.let { renderRating(it) }
      }
    }
  }

  private fun renderTitleDescription(
    movie: Movie,
    translation: Translation?,
    followedState: MovieDetailsUiState.FollowedState?,
    spoilersSettings: SpoilersSettings?,
  ) {
    with(binding) {
      var title = movie.title
      var description = movie.overview

      if (translation?.title?.isNotBlank() == true) {
        title = translation.title
      }
      if (translation?.overview?.isNotBlank() == true) {
        description = translation.overview
      }

      if (followedState == null || spoilersSettings == null) {
        movieDetailsTitle.text = title
        movieDetailsDescription.text = description
        return
      }

      val isMyMovieHidden = spoilersSettings.isMyMoviesHidden && followedState.isMyMovie
      val isWatchlistHidden = spoilersSettings.isWatchlistMoviesHidden && followedState.isWatchlist
      val isHiddenMovieHidden = spoilersSettings.isHiddenMoviesHidden && followedState.isHidden
      val isNotCollectedHidden = spoilersSettings.isNotCollectedMoviesHidden && (!followedState.isInCollection())

      if (isMyMovieHidden || isWatchlistHidden || isHiddenMovieHidden || isNotCollectedHidden) {
        movieDetailsDescription.tag = description
        description = SPOILERS_REGEX.replace(description, SPOILERS_HIDE_SYMBOL)

        if (spoilersSettings.isTapToReveal) {
          with(movieDetailsDescription) {
            onClick {
              tag?.let { text = it.toString() }
              enableFoldOnClick()
            }
          }
        }
      }

      movieDetailsTitle.text = title
      movieDetailsDescription.text = description.ifBlank { getString(R.string.textNoDescription) }
    }
  }

  private fun renderExtraInfo(
    movie: Movie,
    meta: MovieDetailsMeta?,
  ) {
    val country = if (movie.country.isNotBlank()) String.format(ENGLISH, "(%s)", movie.country) else ""
    val releaseDate =
      when {
        movie.released != null -> {
          String.format(
            ENGLISH,
            "%s",
            meta?.dateFormat?.format(movie.released)?.capitalizeWords(),
          )
        }

        movie.year > 0 -> {
          movie.year.toString()
        }

        else -> {
          ""
        }
      }
    val genres =
      movie.genres
        .take(5)
        .mapNotNull { Genre.fromSlug(it) }
        .joinToString(", ") { getString(it.displayName) }

    var extraInfoText =
      getString(
        R.string.textMovieExtraInfo,
        releaseDate,
        country.uppercase(ROOT),
        "⏲ ${movie.runtime}",
        getString(R.string.textMinutesShort),
        genres,
      )

    if (genres.isEmpty()) {
      extraInfoText = extraInfoText.trim().removeSuffix("|")
    }

    binding.movieDetailsExtraInfo.text = extraInfoText
  }

  private fun renderRating(rating: RatingState) {
    with(binding.movieDetailsActions.rateChip) {
      isEnabled = rating.rateLoading == false
      alpha = if (isEnabled) 1.0F else 0.35F

      text =
        if (rating.hasRating()) {
          "${rating.userRating?.rating} / 10"
        } else {
          getString(R.string.textMovieRate)
        }

      onClick {
        openRateDialog()
      }
    }
  }

  private fun renderImage(image: Image) {
    with(binding) {
      if (image.status == UNAVAILABLE) {
        movieDetailsImageProgress.gone()
        movieDetailsPlaceholder.visible()
        movieDetailsImage.isClickable = false
        movieDetailsImage.isEnabled = false
        return
      }
      Glide
        .with(this@MovieDetailsFragment)
        .load(image.fullFileUrl)
        .transform(CenterCrop())
        .transition(withCrossFade(IMAGE_FADE_DURATION_MS))
        .withFailListener {
          movieDetailsImageProgress.gone()
          movieDetailsPlaceholder.visible()
          movieDetailsImage.isClickable = true
          movieDetailsImage.isEnabled = true
        }.withSuccessListener {
          movieDetailsImageProgress.gone()
          movieDetailsPlaceholder.gone()
        }.into(movieDetailsImage)
    }
  }

  private fun renderSnack(event: MessageEvent) {
    if (event.textResId == R.string.errorMalformedMovie) {
      event.consume()?.let {
        val host = (requireActivity() as SnackbarHost).provideSnackbarLayout()
        val snack =
          host.showInfoSnackbar(getString(it), length = Snackbar.LENGTH_INDEFINITE) {
            viewModel.removeMalformedMovie(movieId)
          }
        snackbars.add(snack)
      }
      return
    }
    showSnack(event)
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is OpenDateSelectionSheet -> openDateSelectionSheet(event.movie)
      is RequestWidgetsUpdate -> (requireAppContext() as WidgetsProvider).requestMoviesWidgetsUpdate()
      is Finish -> navigateBack()
    }
  }

  private fun openDateSelectionSheet(movie: Movie) {
    setFragmentResultListener(DateSelectionBottomSheet.REQUEST_DATE_SELECTION) { _, bundle ->
      when (val result = bundle.requireParcelable<Result>(DateSelectionBottomSheet.RESULT_DATE_SELECTION)) {
        is Result.Now -> viewModel.addToMyMovies(isCustomDateSelected = true)
        is Result.CustomDate -> viewModel.addToMyMovies(isCustomDateSelected = true, customDate = result.date)
        is Result.ReleaseDate -> viewModel.addToMyMovies(isCustomDateSelected = true, customDate = result.date)
      }
    }
    val options = DateSelectionBottomSheet.createBundle(movie.released?.atStartOfDay(UTC))
    navigateToSafe(R.id.actionMovieDetailsFragmentToDateSelection, options)
  }

  private fun openShareSheet(movie: Movie) {
    val intent =
      Intent().apply {
        val text =
          "${movie.title}:" +
            "\n" +
            "https://www.imdb.com/title/${movie.ids.imdb.id}"
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
      }

    val shareIntent = Intent.createChooser(intent, "Share ${movie.title}")
    startActivity(shareIntent)
  }

  private fun openRateDialog() {
    setFragmentResultListener(NavigationArgs.REQUEST_RATING) { _, bundle ->
      when (bundle.optionalParcelable<Operation>(NavigationArgs.RESULT)) {
        Operation.SAVE -> renderSnack(MessageEvent.Info(R.string.textRateSaved))
        Operation.REMOVE -> renderSnack(MessageEvent.Info(R.string.textRateRemoved))
        else -> Timber.w("Unknown result.")
      }
      viewModel.loadUserRating()
    }
    val bundle = RatingsBottomSheet.createBundle(movieId, Type.MOVIE)
    navigateToSafe(R.id.actionMovieDetailsFragmentToRating, bundle)
  }

  private fun openListsDialog() {
    setFragmentResultListener(REQUEST_MANAGE_LISTS) { _, _ -> viewModel.loadListsCount() }
    val bundle =
      Bundle().apply {
        putLong(ARG_ID, movieId.id)
        putSerializable(ARG_TYPE, Mode.MOVIES.type)
      }
    navigateToSafe(R.id.actionMovieDetailsFragmentToManageLists, bundle)
  }

  fun showStreamingsView(animate: Boolean) {
    with(binding) {
      if (!animate) {
        movieDetailsStreamingsFragment.visible()
        return
      }
      val animation =
        ConstraintSet().apply {
          clone(movieDetailsMainContent)
          setVisibility(movieDetailsStreamingsFragment.id, View.VISIBLE)
        }
      val transition =
        AutoTransition().apply {
          interpolator = DecelerateInterpolator(1.5F)
          duration = 200
        }
      TransitionManager.beginDelayedTransition(movieDetailsMainContent, transition)
      animation.applyTo(movieDetailsMainContent)
    }
  }

  fun showCollectionsView(animate: Boolean) {
    with(binding) {
      if (!animate) {
        movieDetailsCollectionsFragment.visible()
        return
      }
      val animation =
        ConstraintSet().apply {
          clone(movieDetailsMainContent)
          setVisibility(movieDetailsCollectionsFragment.id, View.VISIBLE)
        }
      val transition =
        AutoTransition().apply {
          interpolator = DecelerateInterpolator(1.5F)
          duration = 200
        }
      TransitionManager.beginDelayedTransition(movieDetailsMainContent, transition)
      animation.applyTo(movieDetailsMainContent)
    }
  }
}
