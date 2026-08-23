package xyz.stignarnia.ui_widgets.theme

import android.content.res.ColorStateList
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import xyz.stignarnia.ui_base.utilities.AndroidVersion

/*
  Every way a WidgetPalette reaches a view, in one file.

  RemoteViews does not hand a widget its views; it records calls and replays them in the launcher's process, so each of these has to be a method the framework has marked as remotable and nothing else will do.
  They are gathered here because the set is small, easily got wrong, and version bound: the tints go through setColorStateList, which arrives in API 31 and is the reason widget theming starts there at all.

  That version check sits inside each function rather than on the call sites.
  Below API 31 no palette is ever resolved - see WidgetPalettes - so these are unreachable there anyway, and keeping the guard here spares every caller a second one.

  Should a background tint ever turn out not to be replayable, this file is the whole of what has to change: the alternative is a pre tinted drawable per palette, as the poster frame already uses.
*/

/** Recolours a shape background - the widget's ground, the label bar, a badge. Needs an opaque fill; see bg_widget_toolbar_tintable. */
internal fun RemoteViews.setBackgroundTint(
  viewId: Int,
  @ColorInt color: Int,
) {
  if (!AndroidVersion.isAtLeastAndroid12) return
  setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color))
}

/** Swaps a shape background and recolours it in one go, for the views whose drawable also changes with the palette. */
internal fun RemoteViews.setBackground(
  viewId: Int,
  @DrawableRes drawableResId: Int,
  @ColorInt color: Int,
) {
  setInt(viewId, "setBackgroundResource", drawableResId)
  setBackgroundTint(viewId, color)
}

/**
 * Recolours an ImageView's contents.
 * setColorFilter rather than an image tint because it is remotable on every version the app supports, and these are flat single colour icons where the two are indistinguishable.
 */
internal fun RemoteViews.setIconTint(
  viewId: Int,
  @ColorInt color: Int,
) = setInt(viewId, "setColorFilter", color)

/** The filled and unfilled halves of a progress bar. */
internal fun RemoteViews.setProgressTint(
  viewId: Int,
  @ColorInt progress: Int,
  @ColorInt track: Int,
) {
  if (!AndroidVersion.isAtLeastAndroid12) return
  setColorStateList(viewId, "setProgressTintList", ColorStateList.valueOf(progress))
  setColorStateList(viewId, "setProgressBackgroundTintList", ColorStateList.valueOf(track))
}
