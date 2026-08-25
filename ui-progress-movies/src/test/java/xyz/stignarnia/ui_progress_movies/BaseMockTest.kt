package xyz.stignarnia.ui_progress_movies

import xyz.stignarnia.common_test.MainDispatcherRule
import xyz.stignarnia.common_test.UnconfinedCoroutineDispatchers
import io.mockk.MockKAnnotations
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Rule

abstract class BaseMockTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()
  protected val testDispatchers = UnconfinedCoroutineDispatchers()

  @Before
  open fun setUp() {
    MockKAnnotations.init(this)
    mockkStatic("androidx.room.RoomDatabaseKt")
  }
}
