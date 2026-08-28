import io.mockk.MockKAnnotations
import io.mockk.mockkStatic
import org.junit.Before

abstract class BaseMockTest {
  @Before
  open fun setUp() {
    MockKAnnotations.init(this)
    mockkStatic("androidx.room.RoomDatabaseKt")
  }
}
