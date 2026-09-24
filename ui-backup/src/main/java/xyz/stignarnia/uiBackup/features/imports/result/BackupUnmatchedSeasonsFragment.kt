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
class BackupUnmatchedSeasonsFragment : Fragment(R.layout.fragment_backup_unmatched_items) {
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
    val showIndex = arguments?.getInt(ARG_SHOW_INDEX, -1) ?: -1
    val show = resultHolder.result?.unmatchedShows?.getOrNull(showIndex)

    with(binding) {
      toolbar.title = show?.title?.resolve(requireContext()) ?: getString(R.string.textBackupUnmatchedShowsTitle)
      toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

      recyclerView.layoutManager = LinearLayoutManager(requireContext())
      recyclerView.adapter = adapter

      val items =
        show?.unmatchedSeasons.orEmpty().mapIndexed { seasonIndex, season ->
          if (season.isEntireSeasonUnmatched) {
            BackupUnmatchedRowUi(
              title = getString(R.string.textBackupUnmatchedSeasonSingle, season.seasonNumber),
              reason = season.reason?.resolve(requireContext()) ?: getString(R.string.textBackupUnmatchedSeasonMissing),
              isClickable = false,
            )
          } else {
            val count = season.unmatchedEpisodes.size
            val subtitle =
              resources.getQuantityString(
                R.plurals.textBackupUnmatchedEpisodesSubtitle,
                count,
                count,
              )
            BackupUnmatchedRowUi(
              title = getString(R.string.textBackupUnmatchedSeasonSingle, season.seasonNumber),
              reason = subtitle,
              isClickable = true,
              onClick = {
                if (findNavController().currentDestination?.id == R.id.backupUnmatchedSeasonsFragment) {
                  val args =
                    Bundle().apply {
                      putInt(BackupUnmatchedEpisodesFragment.ARG_SHOW_INDEX, showIndex)
                      putInt(BackupUnmatchedEpisodesFragment.ARG_SEASON_INDEX, seasonIndex)
                    }
                  findNavController().navigate(R.id.actionBackupUnmatchedSeasonsToEpisodes, args)
                }
              },
            )
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
    const val ARG_SHOW_INDEX = "show_index"
  }
}
