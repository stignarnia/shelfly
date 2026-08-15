package xyz.stignarnia.data_remote.apikey

/**
 * Supplies the API keys used by the remote data sources.
 *
 * Keys are entered by the user at runtime rather than compiled into the binary,
 * so release builds ship with no key material and every install uses its own
 * TMDB quota.
 */
interface ApiKeyProvider {

  /**
   * TMDB v3 API key, sent as an "api_key" query parameter. Empty when unset.
   */
  fun getTmdbApiKey(): String

  /**
   * OMDb API key, used only for IMDb ratings. Empty when unset, in which case
   * ratings are simply unavailable - the app remains usable.
   */
  fun getOmdbApiKey(): String

  fun setTmdbApiKey(key: String)

  fun setOmdbApiKey(key: String)

  /**
   * Whether a TMDB key is available. Without one there is no catalog at all, so
   * this gates the onboarding screen.
   */
  fun hasTmdbApiKey(): Boolean

  /**
   * Whether an OMDb key is available. Without one there are no IMDb, Metascore
   * or Rotten Tomatoes ratings, which the ratings strip says explicitly rather
   * than leaving the values blank for no visible reason.
   */
  fun hasOmdbApiKey(): Boolean
}
