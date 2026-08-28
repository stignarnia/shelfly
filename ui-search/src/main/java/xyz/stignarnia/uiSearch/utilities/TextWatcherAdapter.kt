package xyz.stignarnia.uiSearch.utilities

import android.text.Editable
import android.text.TextWatcher

interface TextWatcherAdapter : TextWatcher {
  override fun beforeTextChanged(
    p0: CharSequence?,
    p1: Int,
    p2: Int,
    p3: Int,
  ) = Unit

  override fun onTextChanged(
    p0: CharSequence?,
    p1: Int,
    p2: Int,
    p3: Int,
  ) = Unit

  override fun afterTextChanged(text: Editable?) = Unit
}
