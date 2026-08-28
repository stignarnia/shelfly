
package xyz.stignarnia.dataLocal.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import xyz.stignarnia.dataLocal.database.dao.helpers.TestData

@RunWith(AndroidJUnit4::class)
class SeasonsDaoTest : BaseDaoTest() {
  @Test
  fun shouldInsertAndStoreSingleEntity() {
    runBlocking {
      val season = TestData.createSeason()

      database.seasonsDao().upsert(listOf(season))
      val result = database.seasonsDao().getAllByShowId(1)
      assertThat(result).containsExactlyElementsIn(listOf(season))
    }
  }

  @Test
  fun shouldInsertAndStoreMultipleEntities() {
    runBlocking {
      val season1 = TestData.createSeason().copy(idTmdb = 1)
      val season2 = TestData.createSeason().copy(idTmdb = 2)

      database.seasonsDao().upsert(listOf(season1, season2))
      val result = database.seasonsDao().getAllByShowId(1)
      assertThat(result).containsExactlyElementsIn(listOf(season1, season2))
    }
  }

  @Test
  fun shouldReturnEntityById() {
    runBlocking {
      val season1 = TestData.createSeason().copy(idTmdb = 1)
      val season2 = TestData.createSeason().copy(idTmdb = 2)

      database.seasonsDao().upsert(listOf(season1, season2))
      val result = database.seasonsDao().getById(2)
      assertThat(result).isEqualTo(season2)
    }
  }

  @Test
  fun shouldReturnEntitiesByIds() {
    runBlocking {
      val season1 = TestData.createSeason().copy(idTmdb = 1)
      val season2 = TestData.createSeason().copy(idTmdb = 2, idShowTmdb = 2)
      val season3 = TestData.createSeason().copy(idTmdb = 3)

      database.seasonsDao().upsert(listOf(season1, season2, season3))
      val result = database.seasonsDao().getAllByShowId(1)
      assertThat(result).containsExactlyElementsIn(listOf(season1, season3))
    }
  }

  @Test
  fun shouldOnlyReturnWatchedSeasons() {
    runBlocking {
      val season1 = TestData.createSeason().copy(idTmdb = 1)
      val season2 = TestData.createSeason().copy(idTmdb = 2)
      val season3 = TestData.createSeason().copy(idTmdb = 3, isWatched = true)

      database.seasonsDao().upsert(listOf(season1, season2, season3))
      val result = database.seasonsDao().getAllWatchedIdsForShows(listOf(1))
      assertThat(result).hasSize(1)
      assertThat(result[0]).isEqualTo(3)
    }
  }

  @Test
  fun shouldReturnNullIfNoEntity() {
    runBlocking {
      val season1 = TestData.createSeason()

      database.seasonsDao().upsert(listOf(season1))
      val result = database.seasonsDao().getById(2)
      assertThat(result).isNull()
    }
  }
}
