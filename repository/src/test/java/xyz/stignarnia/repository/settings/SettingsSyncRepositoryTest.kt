package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.junit.Before
import org.junit.Test

class SettingsSyncRepositoryTest {
  @MockK
  lateinit var sharedPreferences: SharedPreferences

  @MockK
  lateinit var editor: SharedPreferences.Editor

  private lateinit var repository: SettingsSyncRepository

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    every { sharedPreferences.edit() } returns editor
    every { editor.commit() } returns true
    every { editor.apply() } returns Unit
    every { editor.putString(any(), any()) } returns editor
    every { editor.remove(any()) } returns editor

    repository = SettingsSyncRepository(sharedPreferences)
  }

  @Test
  fun `deviceName returns stored preference if present`() {
    every { sharedPreferences.getString("SYNC_DEVICE_NAME", null) } returns "Living Room Tablet"

    assertThat(repository.deviceName).isEqualTo("Living Room Tablet")
  }

  @Test
  fun `deviceName falls back to default and caches it if preference is missing`() {
    every { sharedPreferences.getString("SYNC_DEVICE_NAME", null) } returns null
    val keySlot = slot<String>()
    val valueSlot = slot<String>()
    every { editor.putString(capture(keySlot), capture(valueSlot)) } returns editor

    val name = repository.deviceName

    assertThat(name).isNotEmpty()
    assertThat(keySlot.captured).isEqualTo("SYNC_DEVICE_NAME")
    assertThat(valueSlot.captured).isEqualTo(name)
  }

  @Test
  fun `setting deviceName saves trimmed string`() {
    val keySlot = slot<String>()
    val valueSlot = slot<String>()
    every { editor.putString(capture(keySlot), capture(valueSlot)) } returns editor

    repository.deviceName = "  My Phone  "

    assertThat(keySlot.captured).isEqualTo("SYNC_DEVICE_NAME")
    assertThat(valueSlot.captured).isEqualTo("My Phone")
  }

  @Test
  fun `setting blank deviceName removes preference so it reverts to default`() {
    val keySlot = slot<String>()
    every { editor.remove(capture(keySlot)) } returns editor

    repository.deviceName = "   "

    assertThat(keySlot.captured).isEqualTo("SYNC_DEVICE_NAME")
  }
}
