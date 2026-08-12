package xyz.stignarnia.data_remote

object Config {

  // API keys are supplied at runtime through ApiKeyProvider, not compiled in.
  const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

  const val OMDB_BASE_URL = "https://private.omdbapi.com/"

  const val DISCOVER_LIMIT = 280
  const val ANTICIPATED_LIMIT = 30
  const val RELATED_SHOWS_LIMIT = 30
  const val RELATED_MOVIES_LIMIT = 30
  const val SEARCH_LIMIT = 50
}
