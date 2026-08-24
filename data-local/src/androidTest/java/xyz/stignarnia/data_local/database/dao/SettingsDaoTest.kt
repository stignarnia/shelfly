@file:Suppress("DEPRECATION")

package xyz.stignarnia.data_local.database.dao

import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.data_local.database.dao.helpers.TestData
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDaoTest : BaseDaoTest() {

  @Test
  fun shouldInsertAndSaveData() {
    runBlocking {
      val settings = TestData.createSettings()

      database.settingsDao().upsert(settings)
      val result = database.settingsDao().getAll()
      assertThat(result).isEqualTo(settings)
    }
  }

  // getAll() is declared non-null, so an empty table makes Room throw rather than return null.
  // Callers are expected to guard with getCount() first, the way SettingsRepository.isInitialized() does.
  @Test
  fun shouldReportEmptyCountIfNoEntity() {
    runBlocking {
      assertThat(database.settingsDao().getCount()).isEqualTo(0)

      val result = runCatching { database.settingsDao().getAll() }
      assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }
  }

  @Test
  fun shouldUpdateRowIfAlreadyExists() {
    runBlocking {
      val settings = TestData.createSettings()

      database.settingsDao().upsert(settings)
      assertThat(database.settingsDao().getAll()).isEqualTo(settings)

      val settings2 = settings.copy(myShowsEndedSortBy = "sort")
      database.settingsDao().upsert(settings2)
      assertThat(database.settingsDao().getAll()).isEqualTo(settings2)
    }
  }
}
