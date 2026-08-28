package xyz.stignarnia.uiBase.common

import kotlinx.coroutines.CoroutineScope

interface AppScopeProvider {
  val appScope: CoroutineScope
}
