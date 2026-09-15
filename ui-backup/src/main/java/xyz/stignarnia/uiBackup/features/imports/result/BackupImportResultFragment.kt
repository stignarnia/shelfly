package xyz.stignarnia.uiBackup.features.imports.result

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBackup.R
import xyz.stignarnia.uiBackup.databinding.FragmentBackupImportResultBinding
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.viewBinding
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BackupImportResultFragment : Fragment(R.layout.fragment_backup_import_result) {
  @Inject
  lateinit var resultHolder: BackupImportResultHolder

  private val binding by viewBinding(FragmentBackupImportResultBinding::bind)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupBackPressed()
    setupInsets()
    setupView()
  }

  private fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      findNavController().popBackStack()
    }
  }

  private fun setupView() {
    val result = resultHolder.result

    with(binding) {
      toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
      doneButton.onClick { findNavController().popBackStack() }

      val importedMovies = result?.importedMoviesCount ?: 0
      val importedShows = result?.importedShowsCount ?: 0
      val unmatchedMovies = result?.unmatchedMovies.orEmpty()
      val unmatchedShows = result?.unmatchedShows.orEmpty()
      val unmatchedLists = result?.unmatchedLists.orEmpty()

      moviesImportedCount.text = String.format(Locale.getDefault(), "%d", importedMovies)
      showsImportedCount.text = String.format(Locale.getDefault(), "%d", importedShows)

      val moviesUnmatchedSize = unmatchedMovies.size
      moviesUnmatchedCount.text = String.format(Locale.getDefault(), "%d", moviesUnmatchedSize)
      if (moviesUnmatchedSize > 0) {
        textMoviesUnmatchedHint.text = getString(R.string.textBackupImportResultTapToView)
        arrowMoviesUnmatched.visibility = View.VISIBLE
        cardMoviesUnmatched.isClickable = true
        cardMoviesUnmatched.onClick {
          if (findNavController().currentDestination?.id == R.id.backupImportResultFragment) {
            val args =
              Bundle().apply {
                putString(BackupUnmatchedItemsFragment.ARG_CATEGORY, BackupUnmatchedItemsFragment.CATEGORY_MOVIES)
              }
            findNavController().navigate(R.id.actionBackupImportResultToUnmatched, args)
          }
        }
      } else {
        textMoviesUnmatchedHint.text = getString(R.string.textBackupImportResultAllMatched)
        arrowMoviesUnmatched.visibility = View.GONE
        cardMoviesUnmatched.isClickable = false
      }

      val totalUnmatchedShowsCount =
        unmatchedShows.sumOf { show ->
          if (show.unmatchedSeasons.isEmpty()) 1 else show.unmatchedSeasons.sumOf { it.unmatchedEpisodes.size.coerceAtLeast(1) }
        }
      showsUnmatchedCount.text = String.format(Locale.getDefault(), "%d", totalUnmatchedShowsCount)
      if (totalUnmatchedShowsCount > 0) {
        textShowsUnmatchedHint.text = getString(R.string.textBackupImportResultTapToView)
        arrowShowsUnmatched.visibility = View.VISIBLE
        cardShowsUnmatched.isClickable = true
        cardShowsUnmatched.onClick {
          if (findNavController().currentDestination?.id == R.id.backupImportResultFragment) {
            val args =
              Bundle().apply {
                putString(BackupUnmatchedItemsFragment.ARG_CATEGORY, BackupUnmatchedItemsFragment.CATEGORY_SHOWS)
              }
            findNavController().navigate(R.id.actionBackupImportResultToUnmatched, args)
          }
        }
      } else {
        textShowsUnmatchedHint.text = getString(R.string.textBackupImportResultAllMatched)
        arrowShowsUnmatched.visibility = View.GONE
        cardShowsUnmatched.isClickable = false
      }

      val totalUnmatchedListsCount =
        unmatchedLists.sumOf { list ->
          if (list.unmatchedItems.isEmpty()) 1 else list.unmatchedItems.size
        }
      listsUnmatchedCount.text = String.format(Locale.getDefault(), "%d", totalUnmatchedListsCount)
      if (totalUnmatchedListsCount > 0) {
        textListsUnmatchedHint.text = getString(R.string.textBackupImportResultTapToView)
        arrowListsUnmatched.visibility = View.VISIBLE
        cardListsUnmatched.isClickable = true
        cardListsUnmatched.onClick {
          if (findNavController().currentDestination?.id == R.id.backupImportResultFragment) {
            val args =
              Bundle().apply {
                putString(BackupUnmatchedItemsFragment.ARG_CATEGORY, BackupUnmatchedItemsFragment.CATEGORY_LISTS)
              }
            findNavController().navigate(R.id.actionBackupImportResultToUnmatched, args)
          }
        }
      } else {
        textListsUnmatchedHint.text = getString(R.string.textBackupImportResultAllMatched)
        arrowListsUnmatched.visibility = View.GONE
        cardListsUnmatched.isClickable = false
      }
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { view, insets, padding, _ ->
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
          top = padding.top + inset.top,
          bottom = padding.bottom + inset.bottom,
        )
      }
    }
  }
}
