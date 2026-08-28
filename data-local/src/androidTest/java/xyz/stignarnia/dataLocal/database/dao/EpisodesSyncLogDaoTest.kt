
package xyz.stignarnia.dataLocal.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import xyz.stignarnia.dataLocal.database.model.EpisodesSyncLog

@RunWith(AndroidJUnit4::class)
class EpisodesSyncLogDaoTest : BaseDaoTest() {
  @Test
  fun shouldInsertAndSaveData() {
    runBlocking {
      val testLog = EpisodesSyncLog(1, 999)
      database.episodesSyncLogDao().upsert(testLog)
      val result = database.episodesSyncLogDao().getAll().first()
      assertThat(result).isEqualTo(testLog)
    }
  }

  @Test
  fun shouldUpdateRowIfAlreadyExists() {
    runBlocking {
      val testLog = EpisodesSyncLog(1, 999)

      database.episodesSyncLogDao().upsert(testLog)
      val result = database.episodesSyncLogDao().getAll().first()
      assertThat(result).isEqualTo(testLog)

      val testLog2 = testLog.copy(syncedAt = 888)
      database.episodesSyncLogDao().upsert(testLog2)
      val result2 = database.episodesSyncLogDao().getAll().first()
      assertThat(result2).isEqualTo(testLog2)
    }
  }
}
