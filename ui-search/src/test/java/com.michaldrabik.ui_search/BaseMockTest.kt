package xyz.stignarnia.ui_search

import xyz.stignarnia.common_test.MainDispatcherRule
import xyz.stignarnia.common_test.UnconfinedCoroutineDispatchers
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Rule

@Suppress("EXPERIMENTAL_API_USAGE")
abstract class BaseMockTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()
  protected val testDispatchers = UnconfinedCoroutineDispatchers()

  @Before
  open fun setUp() {
    MockKAnnotations.init(this)
  }
}
