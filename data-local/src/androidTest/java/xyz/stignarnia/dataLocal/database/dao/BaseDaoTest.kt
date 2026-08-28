
package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import xyz.stignarnia.dataLocal.database.AppDatabase

abstract class BaseDaoTest {
  protected lateinit var database: AppDatabase

  @Before
  fun initDb() {
    database =
      Room
        .inMemoryDatabaseBuilder(
          InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
          AppDatabase::class.java,
        ).build()
  }

  @After
  fun closeDb() = database.close()
}
