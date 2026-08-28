package xyz.stignarnia.uiSearch.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.widget.LinearLayout
import xyz.stignarnia.uiSearch.R

class InitialSearchView : LinearLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  init {
    inflate(context, R.layout.view_search_initial, this)
    orientation = VERTICAL
    gravity = CENTER
  }
}
