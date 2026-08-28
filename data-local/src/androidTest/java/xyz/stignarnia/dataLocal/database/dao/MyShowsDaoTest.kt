
package xyz.stignarnia.dataLocal.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.stignarnia.dataLocal.database.dao.helpers.TestData
import xyz.stignarnia.dataLocal.database.model.MyShow
import xyz.stignarnia.dataLocal.database.model.Show

@RunWith(AndroidJUnit4::class)
class MyShowsDaoTest : BaseDaoTest() {
  private val shows = mutableListOf<Show>()

  @Before
  fun setUp() =
    runBlocking {
      shows.add(TestData.createShow().copy(idTmdb = 1))
      shows.add(TestData.createShow().copy(idTmdb = 2))
      shows.add(TestData.createShow().copy(idTmdb = 3))

      database.showsDao().upsert(shows)
    }

  @Test
  fun shouldInsertAndStoreEntities() {
    runBlocking {
      val myShow = MyShow.fromTmdbId(shows[0].idTmdb, 0, 0, 0)

      database.myShowsDao().insert(listOf(myShow))

      val result = database.myShowsDao().getAll()
      assertThat(result).containsExactlyElementsIn(listOf(shows[0]))
    }
  }

  @Test
  fun shouldReturnIdsOnly() {
    runBlocking {
      val myShow1 = MyShow.fromTmdbId(shows[0].idTmdb, 0, 0, 0)
      val myShow2 = MyShow.fromTmdbId(shows[1].idTmdb, 0, 0, 0)

      database.myShowsDao().insert(listOf(myShow1))
      database.myShowsDao().insert(listOf(myShow2))

      val result = database.myShowsDao().getAllTmdbIds()
      assertThat(result).containsExactlyElementsIn(listOf(shows[0].idTmdb, shows[1].idTmdb))
    }
  }

  @Test
  fun shouldReturnMostRecentAddedShows() {
    runBlocking {
      val myShow1 = MyShow.fromTmdbId(shows[0].idTmdb, 0, 0, 0)
      val myShow2 = MyShow.fromTmdbId(shows[1].idTmdb, 999, 999, 999)

      database.myShowsDao().insert(listOf(myShow1))
      database.myShowsDao().insert(listOf(myShow2))

      val result = database.myShowsDao().getAllRecent(10)
      assertThat(result[0]).isEqualTo(shows[1])
      assertThat(result[1]).isEqualTo(shows[0])
    }
  }

  @Test
  fun shouldReturnById() {
    runBlocking {
      val myShow1 = MyShow.fromTmdbId(shows[0].idTmdb, 0, 0, 0)
      val myShow2 = MyShow.fromTmdbId(shows[1].idTmdb, 0, 0, 0)

      database.myShowsDao().insert(listOf(myShow1))
      database.myShowsDao().insert(listOf(myShow2))

      val result = database.myShowsDao().getById(shows[1].idTmdb)
      assertThat(result).isEqualTo(shows[1])
    }
  }

  @Test
  fun shouldDeleteByIdWithoutDeletingParent() {
    runBlocking {
      val myShow1 = MyShow.fromTmdbId(shows[1].idTmdb, 0, 0, 0)

      val showsSize = shows.size
      database.myShowsDao().insert(listOf(myShow1))
      database.myShowsDao().deleteById(shows[1].idTmdb)
      val result = database.myShowsDao().getById(shows[1].idTmdb)

      assertThat(result).isNull()
      assertThat(shows).hasSize(showsSize)
    }
  }
}
