package xyz.stignarnia.uiBase.utilities.extensions

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import java.io.Serializable

fun Fragment.requireString(
  key: String?,
  default: String? = null,
) = requireArguments().getString(key, default)!!

fun Fragment.requireStringArray(key: String?) = requireArguments().getStringArrayList(key)!!

fun Fragment.requireLong(key: String?) = requireArguments().getLong(key)

fun Fragment.requireLongArray(key: String?) = requireArguments().getLongArray(key)!!

fun Fragment.requireBoolean(key: String?) = requireArguments().getBoolean(key)

inline fun <reified T : Serializable> Fragment.requireSerializable(key: String): T =
  requireArguments().requireSerializable(key)

inline fun <reified T : Serializable> Fragment.optionalSerializable(key: String): T? =
  arguments?.optionalSerializable(key)

inline fun <reified T : Parcelable> Fragment.requireParcelable(key: String): T =
  requireArguments().requireParcelable(key)

inline fun <reified T : Parcelable> Fragment.optionalParcelable(key: String): T? = arguments?.optionalParcelable(key)

inline fun <reified T : Parcelable> Bundle.requireParcelable(key: String): T =
  BundleCompat.getParcelable(this, key, T::class.java)!!

inline fun <reified T : Parcelable> Bundle.optionalParcelable(key: String): T? =
  BundleCompat.getParcelable(this, key, T::class.java)

inline fun <reified T : Serializable> Bundle.requireSerializable(key: String): T =
  BundleCompat.getSerializable(this, key, T::class.java)!!

inline fun <reified T : Serializable> Bundle.optionalSerializable(key: String): T? =
  BundleCompat.getSerializable(this, key, T::class.java)
