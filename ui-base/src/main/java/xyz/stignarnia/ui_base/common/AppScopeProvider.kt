package xyz.stignarnia.ui_base.common

import kotlinx.coroutines.CoroutineScope

interface AppScopeProvider {
  val appScope: CoroutineScope
}
