package xyz.stignarnia.common.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts small secrets - API keys, WebDAV credentials - with a key held in the
 * Android Keystore, so they are not stored as plain text in SharedPreferences.
 *
 * Values are encoded as "base64(iv):base64(ciphertext)".
 */
@Singleton
class SecretCipher @Inject constructor() {

  companion object {
    private const val KEYSTORE_TYPE = "AndroidKeyStore"
    private const val KEY_ALIAS = "showly_secrets"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val SEPARATOR = ":"
    private const val BASE64_FLAGS = Base64.NO_WRAP
  }

  fun encrypt(value: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val encrypted = cipher.doFinal(value.toByteArray())
    return encode(cipher.iv) + SEPARATOR + encode(encrypted)
  }

  /**
   * Returns null when the stored value is malformed, or when the Keystore key
   * that encrypted it is gone - which happens after a device restore or when
   * the user's lock screen credentials are reset. Callers treat that as
   * "no value stored" and ask for the secret again.
   */
  fun decrypt(value: String): String? =
    try {
      val parts = value.split(SEPARATOR)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(
        Cipher.DECRYPT_MODE,
        getOrCreateKey(),
        GCMParameterSpec(TAG_LENGTH_BITS, decode(parts[0])),
      )
      String(cipher.doFinal(decode(parts[1])))
    } catch (error: Exception) {
      null
    }

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }
    val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
    if (existing != null) {
      return existing.secretKey
    }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE)
    generator.init(
      KeyGenParameterSpec
        .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build(),
    )
    return generator.generateKey()
  }

  private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, BASE64_FLAGS)

  private fun decode(value: String): ByteArray = Base64.decode(value, BASE64_FLAGS)
}
