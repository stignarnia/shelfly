package xyz.stignarnia.ui_base.utilities

import xyz.stignarnia.common.Mode

interface ModeHost {
  fun setMode(
    mode: Mode,
    force: Boolean = false,
  )

  fun getMode(): Mode
}
