package xyz.stignarnia.ui_show.sections.people.recycler

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.ui_model.Person

class ActorsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private val asyncDiffer = AsyncListDiffer(this, PersonDiffCallback)

  var itemClickListener: (Person) -> Unit = {}

  fun setItems(items: List<Person>) {
    asyncDiffer.submitList(items)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = ViewHolderShow(ActorView(parent.context))

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    (holder.itemView as ActorView).bind(asyncDiffer.currentList[position], itemClickListener)
  }

  override fun getItemCount() = asyncDiffer.currentList.size

  class ViewHolderShow(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView)

  private object PersonDiffCallback : DiffUtil.ItemCallback<Person>() {
    override fun areItemsTheSame(
      oldItem: Person,
      newItem: Person,
    ) = oldItem.ids.tmdb.id == newItem.ids.tmdb.id

    override fun areContentsTheSame(
      oldItem: Person,
      newItem: Person,
    ) = oldItem == newItem
  }
}
