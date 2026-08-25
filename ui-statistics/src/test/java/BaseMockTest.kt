import xyz.stignarnia.common_test.MainDispatcherRule
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Rule

abstract class BaseMockTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Before
  open fun setUp() {
    MockKAnnotations.init(this)
  }
}
