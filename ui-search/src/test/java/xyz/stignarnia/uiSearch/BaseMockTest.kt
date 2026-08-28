package xyz.stignarnia.uiSearch

import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Rule
import xyz.stignarnia.commonTest.MainDispatcherRule
import xyz.stignarnia.commonTest.UnconfinedCoroutineDispatchers

abstract class BaseMockTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()
  protected val testDispatchers = UnconfinedCoroutineDispatchers()

  @Before
  open fun setUp() {
    MockKAnnotations.init(this)
  }
}
