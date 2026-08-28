package xyz.stignarnia.uiSearch.recycler.suggestions

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.BaseAdapter
import xyz.stignarnia.uiSearch.recycler.SearchListItem
import xyz.stignarnia.uiSearch.views.SearchSuggestionView

class SuggestionAdapter(
  private val itemClickListener: (SearchListItem) -> Unit,
  private val missingImageListener: (SearchListItem, Boolean) -> Unit,
  private val missingTranslationListener: (SearchListItem) -> Unit,
) : BaseAdapter<SearchListItem>() {
  override val asyncDiffer = AsyncListDiffer(this, SuggestionItemDiffCallback())

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = BaseViewHolder(
    SearchSuggestionView(parent.context).apply {
      itemClickListener = this@SuggestionAdapter.itemClickListener
      missingImageListener = this@SuggestionAdapter.missingImageListener
      missingTranslationListener = this@SuggestionAdapter.missingTranslationListener
    },
  )

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    val item = asyncDiffer.currentList[position]
    (holder.itemView as SearchSuggestionView).bind(item)
  }
}
