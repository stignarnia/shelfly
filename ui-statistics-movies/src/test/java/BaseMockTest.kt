import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Rule
import xyz.stignarnia.commonTest.MainDispatcherRule

abstract class BaseMockTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Before
  open fun setUp() {
    MockKAnnotations.init(this)
  }
}
