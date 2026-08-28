package xyz.stignarnia.uiMovie.sections.collections.list.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiModel.MovieCollection
import xyz.stignarnia.uiMovie.databinding.ViewMovieCollectionBinding

class MovieCollectionItemView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewMovieCollectionBinding.inflate(LayoutInflater.from(context), this)

  private lateinit var item: MovieCollection
  var itemClickListener: ((MovieCollection) -> Unit)? = null

  init {
    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    clipChildren = false
    binding.root.onClick {
      itemClickListener?.invoke(item)
    }
  }

  fun bind(item: MovieCollection) {
    this.item = item
    with(binding) {
      title.text = item.name
      description.text = item.description.ifBlank { item.name }
    }
  }
}
