package xyz.stignarnia.uiBackup.features.imports.result

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBackup.R
import xyz.stignarnia.uiBackup.databinding.FragmentBackupUnmatchedItemsBinding
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class BackupUnmatchedItemsFragment : Fragment(R.layout.fragment_backup_unmatched_items) {
  private val binding by viewBinding(FragmentBackupUnmatchedItemsBinding::bind)

  @Inject
  internal lateinit var resultHolder: BackupImportResultHolder

  private val adapter = BackupUnmatchedAdapter()

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupBackPressed()
    setupView()
    setupInsets()
  }

  private fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      findNavController().popBackStack()
    }
  }

  private fun setupView() {
    val category = arguments?.getString(ARG_CATEGORY) ?: CATEGORY_MOVIES
    val isMovies = category == CATEGORY_MOVIES

    with(binding) {
      toolbar.title =
        getString(
          if (isMovies) R.string.textBackupUnmatchedMoviesTitle else R.string.textBackupUnmatchedShowsTitle,
        )
      toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

      recyclerView.layoutManager = LinearLayoutManager(requireContext())
      recyclerView.adapter = adapter

      val items =
        if (isMovies) {
          resultHolder.result?.unmatchedMovies.orEmpty().map { item ->
            BackupUnmatchedRowUi(
              title = item.title,
              reason = item.reason,
              isClickable = false,
            )
          }
        } else {
          resultHolder.result?.unmatchedShows.orEmpty().mapIndexed { index, show ->
            val hasSeasons = show.unmatchedSeasons.isNotEmpty()
            if (!hasSeasons) {
              BackupUnmatchedRowUi(
                title = show.title,
                reason = show.reason ?: getString(R.string.textBackupUnmatchedSeasonMissing),
                isClickable = false,
              )
            } else {
              val count = show.unmatchedSeasons.size
              val subtitle =
                resources.getQuantityString(
                  R.plurals.textBackupUnmatchedSeasonsSubtitle,
                  count,
                  count,
                )
              val reasonText =
                if (show.reason != null) {
                  "${show.reason} • $subtitle"
                } else {
                  subtitle
                }
              BackupUnmatchedRowUi(
                title = show.title,
                reason = reasonText,
                isClickable = true,
                onClick = {
                  if (findNavController().currentDestination?.id == R.id.backupUnmatchedItemsFragment) {
                    val args =
                      Bundle().apply {
                        putInt(BackupUnmatchedSeasonsFragment.ARG_SHOW_INDEX, index)
                      }
                    findNavController().navigate(R.id.actionBackupUnmatchedItemsToSeasons, args)
                  }
                },
              )
            }
          }
        }

      adapter.submitList(items)
      recyclerView.visibleIf(items.isNotEmpty(), gone = true)
      emptyView.visibleIf(items.isEmpty(), gone = true)
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

  companion object {
    const val ARG_CATEGORY = "category"
    const val CATEGORY_MOVIES = "movies"
    const val CATEGORY_SHOWS = "shows"
  }
}
