
package xyz.stignarnia.dataLocal.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import xyz.stignarnia.dataLocal.database.dao.helpers.TestData
import xyz.stignarnia.dataLocal.database.model.WatchlistShow

@RunWith(AndroidJUnit4::class)
class WatchlistShowsDaoTest : BaseDaoTest() {
  @Test
  fun shouldInsertAndStoreSingleEntity() {
    runBlocking {
      val show = TestData.createShow()
      val seeLaterShow = WatchlistShow.fromTmdbId(show.idTmdb, 999)

      database.showsDao().upsert(listOf(show))
      database.watchlistShowsDao().insert(seeLaterShow)
      val result = database.watchlistShowsDao().getAll()
      assertThat(result).containsExactlyElementsIn(listOf(show.copy(updatedAt = 999, createdAt = 999)))
    }
  }

  @Test
  fun shouldInsertAndStoreSingleEntityById() {
    runBlocking {
      val show = TestData.createShow()
      val show2 = TestData.createShow().copy(idTmdb = 2)
      val seeLaterShow = WatchlistShow.fromTmdbId(show.idTmdb, 999)
      val seeLaterShow2 = WatchlistShow.fromTmdbId(show2.idTmdb, 999)

      database.showsDao().upsert(listOf(show, show2))
      database.watchlistShowsDao().insert(seeLaterShow)
      database.watchlistShowsDao().insert(seeLaterShow2)

      val result = database.watchlistShowsDao().getById(2)
      assertThat(result).isEqualTo(show2)
    }
  }

  @Test
  fun shouldDeleteSingleEntityById() {
    runBlocking {
      val show = TestData.createShow()
      val show2 = TestData.createShow().copy(idTmdb = 2)
      val seeLaterShow = WatchlistShow.fromTmdbId(show.idTmdb, 999)
      val seeLaterShow2 = WatchlistShow.fromTmdbId(show2.idTmdb, 999)

      database.showsDao().upsert(listOf(show, show2))
      database.watchlistShowsDao().insert(seeLaterShow)
      database.watchlistShowsDao().insert(seeLaterShow2)
      database.watchlistShowsDao().deleteById(2)

      val result = database.watchlistShowsDao().getAll()
      assertThat(result).containsExactlyElementsIn(listOf(show.copy(updatedAt = 999, createdAt = 999)))
    }
  }
}
