package xyz.stignarnia.data_remote.apikey

import android.content.SharedPreferences
import xyz.stignarnia.common.security.SecretCipher
import xyz.stignarnia.data_remote.BuildConfig
import javax.inject.Singleton

/**
 * Stores API keys encrypted in SharedPreferences.
 *
 * Falls back to the build-time key when nothing is stored. That value is empty
 * in release builds and only populated in debug builds from local.properties,
 * so development does not mean retyping a key after every install.
 */
@Singleton
internal class PreferencesApiKeyProvider(
  private val sharedPreferences: SharedPreferences,
  private val secretCipher: SecretCipher,
) : ApiKeyProvider {

  companion object {
    private const val KEY_TMDB = "TMDB_API_KEY"
    private const val KEY_OMDB = "OMDB_API_KEY"
  }

  private var tmdbKey: String? = null
  private var omdbKey: String? = null

  override fun getTmdbApiKey(): String {
    tmdbKey?.let { return it }
    return read(KEY_TMDB, BuildConfig.TMDB_API_KEY).also { tmdbKey = it }
  }

  override fun getOmdbApiKey(): String {
    omdbKey?.let { return it }
    return read(KEY_OMDB, BuildConfig.OMDB_API_KEY).also { omdbKey = it }
  }

  override fun setTmdbApiKey(key: String) {
    tmdbKey = key.trim()
    write(KEY_TMDB, key.trim())
  }

  override fun setOmdbApiKey(key: String) {
    omdbKey = key.trim()
    write(KEY_OMDB, key.trim())
  }

  override fun hasTmdbApiKey(): Boolean = getTmdbApiKey().isNotBlank()

  private fun read(
    key: String,
    fallback: String,
  ): String {
    val stored = sharedPreferences.getString(key, null) ?: return fallback
    return secretCipher.decrypt(stored) ?: fallback
  }

  private fun write(
    key: String,
    value: String,
  ) {
    val editor = sharedPreferences.edit()
    if (value.isBlank()) {
      editor.remove(key)
    } else {
      editor.putString(key, secretCipher.encrypt(value))
    }
    editor.apply()
  }
}
