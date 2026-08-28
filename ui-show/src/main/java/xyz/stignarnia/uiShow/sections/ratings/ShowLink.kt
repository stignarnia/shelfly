package xyz.stignarnia.uiShow.sections.ratings

import android.net.Uri
import xyz.stignarnia.uiBase.common.AppCountry

enum class ShowLink {
  IMDB,
  TVDB,
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

    TVDB -> {
      "https://www.thetvdb.com/?id=$id&tab=series"
    }

    TMDB -> {
      "https://www.themoviedb.org/tv/$id"
    }

    METACRITIC -> {
      "https://www.metacritic.com/search/$id?category=1"
    }

    ROTTEN -> {
      "https://www.rottentomatoes.com/search?search=$id"
    }

    JUST_WATCH -> {
      "https://www.justwatch.com/${country.code}/${country.justWatchQuery}?content_type=show&q=${
        Uri.encode(
          id,
        )
      }"
    }
  }
}
