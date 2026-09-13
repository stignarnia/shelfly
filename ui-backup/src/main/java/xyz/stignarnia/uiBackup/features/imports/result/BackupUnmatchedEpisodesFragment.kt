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
class BackupUnmatchedEpisodesFragment : Fragment(R.layout.fragment_backup_unmatched_items) {
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
    val seasonIndex = arguments?.getInt(ARG_SEASON_INDEX, -1) ?: -1

    val show = resultHolder.result?.unmatchedShows?.getOrNull(showIndex)
    val season = show?.unmatchedSeasons?.getOrNull(seasonIndex)

    with(binding) {
      toolbar.title =
        if (season != null) {
          getString(R.string.textBackupUnmatchedSeasonSingle, season.seasonNumber)
        } else {
          getString(R.string.textBackupUnmatchedShowsTitle)
        }
      toolbar.subtitle = show?.title
      toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

      recyclerView.layoutManager = LinearLayoutManager(requireContext())
      recyclerView.adapter = adapter

      val items =
        season?.unmatchedEpisodes.orEmpty().map { episode ->
          BackupUnmatchedRowUi(
            title = episode.title ?: getString(R.string.textBackupUnmatchedEpisodeSingle, episode.episodeNumber),
            reason = episode.reason,
            isClickable = false,
          )
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
    const val ARG_SEASON_INDEX = "season_index"
  }
}
