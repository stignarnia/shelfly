package xyz.stignarnia.uiBackup.features.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class BackupFileNameTest {
  @Test
  fun `Should read the date back out of a backup name`() {
    val date = BackupFileName.parseDate("shelfly_export_20260924153007.json")

    assertThat(date).isEqualTo(LocalDateTime.of(2026, 9, 24, 15, 30, 7))
  }

  @Test
  fun `Should read the date out of a legacy backup name`() {
    val date = BackupFileName.parseDate("showly_export_20240101000000.json")

    assertThat(date).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
  }

  @Test
  fun `Should round trip a created name`() {
    assertThat(BackupFileName.parseDate(BackupFileName.create())).isNotNull()
  }

  @Test
  fun `Should not date a name in any other shape`() {
    assertThat(BackupFileName.parseDate("shelfly_export_backup.json")).isNull()
    assertThat(BackupFileName.parseDate("shelfly_export_20261399000000.json")).isNull()
    assertThat(BackupFileName.parseDate("notes_20260924153007.json")).isNull()
  }
}
