package xyz.stignarnia.uiMovie.helpers

import android.net.Uri
import xyz.stignarnia.uiBase.common.AppCountry

enum class MovieLink {
  IMDB,
  TMDB,
  METACRITIC,
  ROTTEN,
  JUST_WATCH,
  ;

  fun getUri(
    id: String,
    country: AppCountry,
  ) = when (this) {
    IMDB -> {
      "https://www.imdb.com/title/$id"
    }

    TMDB -> {
      "https://www.themoviedb.org/movie/$id"
    }

    METACRITIC -> {
      "https://www.metacritic.com/search/$id?category=2"
    }

    ROTTEN -> {
      "https://www.rottentomatoes.com/search?search=$id"
    }

    JUST_WATCH -> {
      "https://www.justwatch.com/${country.code}/${country.justWatchQuery}" +
        "?content_type=movie&q=${Uri.encode(id)}"
    }
  }
}
