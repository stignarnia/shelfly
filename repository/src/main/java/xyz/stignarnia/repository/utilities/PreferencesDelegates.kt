package xyz.stignarnia.repository.utilities

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StringPreference(
  private val sharedPreferences: SharedPreferences,
  private val key: String,
  private val defaultValue: String,
) : ReadWriteProperty<Any, String> {

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>,
  ): String = sharedPreferences.getString(key, defaultValue) ?: defaultValue

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: String,
  ) {
    sharedPreferences.edit { putString(key, value) }
  }
}

class BooleanPreference(
  private val sharedPreferences: SharedPreferences,
  private val key: String,
  private val defaultValue: Boolean = false,
) : ReadWriteProperty<Any, Boolean> {

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>,
  ): Boolean = sharedPreferences.getBoolean(key, defaultValue)

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: Boolean,
  ) {
    sharedPreferences.edit { putBoolean(key, value) }
  }
}

class IntPreference(
  private val sharedPreferences: SharedPreferences,
  private val key: String,
  private val defaultValue: Int = 0,
) : ReadWriteProperty<Any, Int> {

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>,
  ): Int = sharedPreferences.getInt(key, defaultValue)

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: Int,
  ) {
    sharedPreferences.edit { putInt(key, value) }
  }
}

class LongPreference(
  private val sharedPreferences: SharedPreferences,
  private val key: String,
  private val defaultValue: Long = 0,
) : ReadWriteProperty<Any, Long> {

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>,
  ): Long = sharedPreferences.getLong(key, defaultValue)

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: Long,
  ) {
    sharedPreferences.edit { putLong(key, value) }
  }
}

class EnumPreference<T : Enum<T>>(
  private val sharedPreferences: SharedPreferences,
  private val key: String,
  private val defaultValue: T,
  private val clazz: Class<T>,
) : ReadWriteProperty<Any, T> {

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>,
  ): T {
    val enumName = sharedPreferences.getString(key, "")
    // enumConstants is a platform type and is null for a non-enum class; T is bounded to Enum here, so the elvis only ever covers a missing or unknown stored name.
    return clazz.enumConstants?.find { it.name == enumName } ?: defaultValue
  }

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: T,
  ) {
    sharedPreferences.edit { putString(key, value.name) }
  }
}
