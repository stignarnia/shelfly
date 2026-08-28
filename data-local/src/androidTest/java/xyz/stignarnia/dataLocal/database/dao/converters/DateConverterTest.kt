
package xyz.stignarnia.dataLocal.database.dao.converters

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.stignarnia.dataLocal.database.converters.DateConverter
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class DateConverterTest {
  private val SUT by lazy { DateConverter() }

  @Before
  fun setUp() {
  }

  @Test
  fun shouldConvertTimestampToDate() {
    val date = SUT.stringToDate(1573120000000) // Thu Nov 07 2019 09:46:40
    assertThat(date).isEqualTo(ZonedDateTime.of(2019, 11, 7, 9, 46, 40, 0, ZoneId.of("UTC")))
  }

  @Test
  fun shouldConvertDateToTimestamp() {
    val timestamp = SUT.dateToString(ZonedDateTime.of(2019, 11, 7, 9, 46, 40, 0, ZoneId.of("UTC")))
    assertThat(timestamp).isEqualTo(1573120000000)
  }
}
