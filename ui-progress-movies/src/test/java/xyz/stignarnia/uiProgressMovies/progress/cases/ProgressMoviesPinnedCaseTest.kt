package xyz.stignarnia.uiProgressMovies.progress.cases

import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.repository.PinnedItemsRepository
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiProgressMovies.BaseMockTest

class ProgressMoviesPinnedCaseTest : BaseMockTest() {
  @RelaxedMockK lateinit var pinnedItemsRepository: PinnedItemsRepository

  private lateinit var SUT: ProgressMoviesPinnedCase

  @Before
  override fun setUp() {
    super.setUp()
    SUT = ProgressMoviesPinnedCase(pinnedItemsRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `Should set pinned item properly`() =
    runTest {
      SUT.addPinnedItem(Movie.EMPTY)

      coVerify(exactly = 1) { pinnedItemsRepository.addPinnedItem(Movie.EMPTY) }
    }

  @Test
  fun `Should remove pinned item properly`() =
    runTest {
      SUT.removePinnedItem(Movie.EMPTY)

      coVerify(exactly = 1) { pinnedItemsRepository.removePinnedItem(Movie.EMPTY) }
    }
}
