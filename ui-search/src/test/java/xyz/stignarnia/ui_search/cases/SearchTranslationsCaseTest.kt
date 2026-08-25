package xyz.stignarnia.ui_search.cases

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_search.BaseMockTest
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SearchTranslationsCaseTest : BaseMockTest() {

  @RelaxedMockK lateinit var translationsRepository: TranslationsRepository

  private lateinit var SUT: SearchTranslationsCase

  @Before
  override fun setUp() {
    super.setUp()
    SUT = SearchTranslationsCase(testDispatchers, translationsRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `Should return empty show translation if language is default`() =
    runTest {
      coEvery { translationsRepository.getLanguage() } returns "en"

      val item = Show.EMPTY
      val result = SUT.loadTranslation(item)

      assertThat(result).isEqualTo(Translation.EMPTY)
      coVerify(exactly = 1) { translationsRepository.getLanguage() }
      confirmVerified(translationsRepository)
    }

  @Test
  fun `Should return empty movie translation if language is default`() =
    runTest {
      coEvery { translationsRepository.getLanguage() } returns "en"

      val item = Movie.EMPTY
      val result = SUT.loadTranslation(item)

      assertThat(result).isEqualTo(Translation.EMPTY)
      coVerify(exactly = 1) { translationsRepository.getLanguage() }
      confirmVerified(translationsRepository)
    }

  @Test
  fun `Should return show translation if language is not default`() =
    runTest {
      val item = Show.EMPTY
      coEvery { translationsRepository.getLanguage() } returns "pl"
      coEvery { translationsRepository.loadTranslation(item, any(), any()) } returns Translation.EMPTY

      val result = SUT.loadTranslation(item)

      assertThat(result).isNotNull()
      coVerify(exactly = 1) { translationsRepository.getLanguage() }
      coVerify(exactly = 1) { translationsRepository.loadTranslation(item, any(), any()) }
      confirmVerified(translationsRepository)
    }

  @Test
  fun `Should return movie translation if language is not default`() =
    runTest {
      val item = Movie.EMPTY
      coEvery { translationsRepository.getLanguage() } returns "pl"
      coEvery { translationsRepository.loadTranslation(item, any(), any()) } returns Translation.EMPTY

      val result = SUT.loadTranslation(item)

      assertThat(result).isNotNull()
      coVerify(exactly = 1) { translationsRepository.getLanguage() }
      coVerify(exactly = 1) { translationsRepository.loadTranslation(item, any(), any()) }
      confirmVerified(translationsRepository)
    }
}
