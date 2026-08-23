package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.junit.Before
import org.junit.Test

class SettingsWidgetsRepositoryTest {

  @MockK lateinit var sharedPreferences: SharedPreferences
  @MockK lateinit var editor: SharedPreferences.Editor

  private lateinit var repository: SettingsWidgetsRepository

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    every { sharedPreferences.edit() } returns editor
    every { editor.commit() } returns true
    every { editor.apply() } returns Unit
    every { editor.putString(any(), any()) } returns editor
    every { editor.putInt(any(), any()) } returns editor
    every { editor.remove(any()) } returns editor

    repository = SettingsWidgetsRepository(sharedPreferences)
  }

  @Test
  fun `getWidgetTransparency returns stored value when present`() {
    every { sharedPreferences.getInt("WIDGET_TRANSPARENCY123", 0) } returns 45

    val transparency = repository.getWidgetTransparency(123)

    assertThat(transparency).isEqualTo(45)
  }

  @Test
  fun `getWidgetTransparency returns default 0 when not stored`() {
    every { sharedPreferences.getInt("WIDGET_TRANSPARENCY123", 0) } returns 0

    val transparency = repository.getWidgetTransparency(123)

    assertThat(transparency).isEqualTo(0)
  }

  @Test
  fun `setWidgetTransparency clamps value between 0 and 100`() {
    val keySlot = slot<String>()
    val valueSlot = slot<Int>()
    every { editor.putInt(capture(keySlot), capture(valueSlot)) } returns editor

    repository.setWidgetTransparency(123, 150)
    assertThat(keySlot.captured).isEqualTo("WIDGET_TRANSPARENCY123")
    assertThat(valueSlot.captured).isEqualTo(100)

    repository.setWidgetTransparency(123, -20)
    assertThat(valueSlot.captured).isEqualTo(0)
  }

  @Test
  fun `clearWidget removes all keys for the widget id`() {
    val removedKeys = mutableListOf<String>()
    every { editor.remove(capture(removedKeys)) } returns editor

    repository.clearWidget(42)

    assertThat(removedKeys).containsExactly(
      "WIDGET_CALENDAR_MODE42",
      "WIDGET_CALENDAR_MOVIES_MODE42",
      "WIDGET_THEME42",
      "WIDGET_AMOLED42",
      "WIDGET_TRANSPARENCY42",
    )
  }
}
