package xyz.stignarnia.repository.common

import xyz.stignarnia.common_test.UnconfinedCoroutineDispatchers
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.R
import xyz.stignarnia.repository.mappers.CustomListMapper
import xyz.stignarnia.repository.mappers.EpisodeMapper
import xyz.stignarnia.repository.mappers.IdsMapper
import xyz.stignarnia.repository.mappers.ImageMapper
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.mappers.MovieMapper
import xyz.stignarnia.repository.mappers.PersonMapper
import xyz.stignarnia.repository.mappers.RatingsMapper
import xyz.stignarnia.repository.mappers.SeasonMapper
import xyz.stignarnia.repository.mappers.SettingsMapper
import xyz.stignarnia.repository.mappers.ShowMapper
import xyz.stignarnia.repository.mappers.StreamingsMapper
import xyz.stignarnia.repository.mappers.TranslationMapper
import xyz.stignarnia.repository.mappers.UserRatingsMapper
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Before

abstract class BaseMockTest {

  @MockK lateinit var database: LocalDataSource
  @MockK lateinit var transactions: TransactionsProvider
  @MockK lateinit var cloud: RemoteDataSource

  protected val testDispatchers = UnconfinedCoroutineDispatchers()
  private val idsMapper = IdsMapper()
  private val episodeMapper = EpisodeMapper(idsMapper)

  val mappers = Mappers(
    idsMapper,
    ImageMapper(),
    ShowMapper(idsMapper),
    MovieMapper(idsMapper),
    episodeMapper,
    SeasonMapper(idsMapper, episodeMapper),
    PersonMapper(),
    SettingsMapper(),
    TranslationMapper(idsMapper),
    CustomListMapper(),
    RatingsMapper(),
    UserRatingsMapper(),
    StreamingsMapper(),
  )

  @Before
  open fun setUp() {
    MockKAnnotations.init(this)
    clearAllMocks()
    mockkStatic("androidx.room.RoomDatabaseKt")
    val lambda = slot<suspend () -> R>()
    coEvery { transactions.withTransaction(capture(lambda)) } coAnswers { lambda.captured.invoke() }
  }
}
