package xyz.stignarnia.common.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

interface CoroutineDispatchers {
  val Main: CoroutineDispatcher
  val IO: CoroutineDispatcher
  val Default: CoroutineDispatcher
  val Unconfined: CoroutineDispatcher
}
