package xyz.stignarnia.data_remote

object Config {

  // API keys are supplied at runtime through ApiKeyProvider, not compiled in.
  const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

  // private.omdbapi.com only accepts OMDb's Patreon-tier keys and rejects every other key as invalid.
  // Upstream Showly shipped a patron key; this fork asks users for their own, which are almost always free-tier and www-only.
  const val OMDB_BASE_URL = "https://www.omdbapi.com/"

  const val DISCOVER_LIMIT = 280
  const val ANTICIPATED_LIMIT = 30
  const val RELATED_SHOWS_LIMIT = 30
  const val RELATED_MOVIES_LIMIT = 30
  const val SEARCH_LIMIT = 50
}
