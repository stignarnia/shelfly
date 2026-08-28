
package xyz.stignarnia.dataLocal.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import xyz.stignarnia.dataLocal.database.dao.helpers.TestData

@RunWith(AndroidJUnit4::class)
class ShowsDaoTest : BaseDaoTest() {
  @Test
  fun shouldInsertAndStoreSingleEntity() {
    runBlocking {
      val show = TestData.createShow()

      database.showsDao().upsert(listOf(show))
      val result = database.showsDao().getAll()
      assertThat(result).hasSize(1)
      assertThat(result.first()).isEqualTo(show)
    }
  }

  @Test
  fun shouldInsertAndStoreMultipleEntities() {
    runBlocking {
      val show1 = TestData.createShow().copy(idTmdb = 1)
      val show2 = TestData.createShow().copy(idTmdb = 2)

      database.showsDao().upsert(listOf(show1, show2))
      val result = database.showsDao().getAll()
      assertThat(result).hasSize(2)
      assertThat(result[0]).isEqualTo(show1)
      assertThat(result[1]).isEqualTo(show2)
    }
  }

  @Test
  fun shouldReturnEntityById() {
    runBlocking {
      val show1 = TestData.createShow().copy(idTmdb = 1)
      val show2 = TestData.createShow().copy(idTmdb = 2)

      database.showsDao().upsert(listOf(show1, show2))
      val result = database.showsDao().getById(2)
      assertThat(result).isEqualTo(show2)
    }
  }

  @Test
  fun shouldReturnEntitiesByIds() {
    runBlocking {
      val show1 = TestData.createShow().copy(idTmdb = 1)
      val show2 = TestData.createShow().copy(idTmdb = 2)
      val show3 = TestData.createShow().copy(idTmdb = 3)

      database.showsDao().upsert(listOf(show1, show2, show3))
      val result = database.showsDao().getAll(listOf(1, 3))
      assertThat(result).containsExactlyElementsIn(listOf(show1, show3))
    }
  }

  @Test
  fun shouldReturnNullIfNoEntity() {
    runBlocking {
      val show1 = TestData.createShow()

      database.showsDao().upsert(listOf(show1))
      val result = database.showsDao().getById(2)
      assertThat(result).isNull()
    }
  }
}
