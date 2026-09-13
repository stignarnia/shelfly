package xyz.stignarnia.uiBackup.features.imports.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBackup.databinding.ItemBackupUnmatchedBinding
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf

internal data class BackupUnmatchedRowUi(
  val title: String,
  val reason: String,
  val isClickable: Boolean = false,
  val onClick: (() -> Unit)? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BackupUnmatchedRowUi) return false
    return title == other.title && reason == other.reason && isClickable == other.isClickable
  }

  override fun hashCode(): Int {
    var result = title.hashCode()
    result = 31 * result + reason.hashCode()
    result = 31 * result + isClickable.hashCode()
    return result
  }
}

internal class BackupUnmatchedAdapter :
  ListAdapter<BackupUnmatchedRowUi, BackupUnmatchedAdapter.ViewHolder>(DiffCallback) {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val binding =
      ItemBackupUnmatchedBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false,
      )
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    holder.bind(getItem(position))
  }

  internal class ViewHolder(
    private val binding: ItemBackupUnmatchedBinding,
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: BackupUnmatchedRowUi) {
      binding.unmatchedTitle.text = item.title
      binding.unmatchedReason.text = item.reason
      binding.unmatchedArrow.visibleIf(item.isClickable, gone = true)
      binding.root.isClickable = item.isClickable
      binding.root.isFocusable = item.isClickable
      if (item.isClickable && item.onClick != null) {
        binding.root.setOnClickListener { item.onClick.invoke() }
      } else {
        binding.root.setOnClickListener(null)
      }
    }
  }

  private object DiffCallback : DiffUtil.ItemCallback<BackupUnmatchedRowUi>() {
    override fun areItemsTheSame(
      oldItem: BackupUnmatchedRowUi,
      newItem: BackupUnmatchedRowUi,
    ): Boolean = oldItem.title == newItem.title && oldItem.reason == newItem.reason

    override fun areContentsTheSame(
      oldItem: BackupUnmatchedRowUi,
      newItem: BackupUnmatchedRowUi,
    ): Boolean = oldItem == newItem
  }
}
