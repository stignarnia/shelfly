package xyz.stignarnia.ui_base.utilities

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.chip.Chip
import xyz.stignarnia.common.Config
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_model.StreamingProvider

/**
 * The discover streaming filter offers whatever TMDB lists for the user's region, so unlike the fixed network chips there is no bundled icon to show - the logo is fetched with the rest of the entry.
 */
object StreamingProviderChips {

  fun create(
    fragment: Fragment,
    provider: StreamingProvider,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
  ): Chip {
    val context = fragment.requireContext()
    val iconSize = context.dimenToPx(R.dimen.discoverFilterProviderIcon)

    return Chip(context).apply {
      tag = provider
      text = provider.name
      isCheckable = true
      isCheckedIconVisible = false
      shapeAppearanceModel = shapeAppearanceModel
        .toBuilder()
        .setAllCornerSizes(100f)
        .build()
      setEnsureMinTouchTargetSize(false)
      chipBackgroundColor =
        ContextCompat.getColorStateList(context, R.color.selector_discover_chip_background)
      setChipStrokeColorResource(R.color.selector_discover_chip_stroke)
      setChipStrokeWidthResource(R.dimen.discoverFilterChipStroke)
      setTextColor(ContextCompat.getColorStateList(context, R.color.selector_discover_chip_text))
      this.isChecked = isChecked
      setOnCheckedChangeListener { _, _ -> onCheckedChange() }

      if (provider.logoPath.isNotBlank()) {
        chipIconSize = iconSize.toFloat()
        Glide
          .with(fragment)
          .load("${Config.TMDB_IMAGE_BASE_LOGO_URL}${provider.logoPath}")
          .transform(CenterInside(), RoundedCorners(iconSize / 4))
          .into(
            object : CustomTarget<Drawable>(iconSize, iconSize) {
              override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?,
              ) {
                chipIcon = resource
                isChipIconVisible = true
              }

              override fun onLoadCleared(placeholder: Drawable?) {
                chipIcon = null
                isChipIconVisible = false
              }
            },
          )
      }
    }
  }
}
