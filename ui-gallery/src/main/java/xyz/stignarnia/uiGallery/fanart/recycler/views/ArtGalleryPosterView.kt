package xyz.stignarnia.uiGallery.fanart.recycler.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.withFailListener
import xyz.stignarnia.uiBase.utilities.extensions.withSuccessListener
import xyz.stignarnia.uiGallery.R
import xyz.stignarnia.uiGallery.databinding.ViewGalleryPosterImageBinding
import xyz.stignarnia.uiModel.Image

class ArtGalleryPosterView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewGalleryPosterImageBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
  }

  private val cornerRadius by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  var onItemClickListener: (() -> Unit)? = null

  fun bind(image: Image) {
    clear()
    with(binding) {
      viewGalleryPosterImage.onClick { onItemClickListener?.invoke() }
      viewGalleryPosterImageProgress.visible()
    }
    loadImage(image)
  }

  private fun loadImage(image: Image) {
    with(binding) {
      Glide
        .with(this@ArtGalleryPosterView)
        .load(image.fullFileUrl)
        .transform(CenterCrop(), RoundedCorners(cornerRadius))
        .withFailListener { viewGalleryPosterImageProgress.gone() }
        .withSuccessListener { viewGalleryPosterImageProgress.gone() }
        .into(viewGalleryPosterImage)
    }
  }

  private fun clear() {
    binding.viewGalleryPosterImageProgress.gone()
    Glide.with(this)
  }
}
