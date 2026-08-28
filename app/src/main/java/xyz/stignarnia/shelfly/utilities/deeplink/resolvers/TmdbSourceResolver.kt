package xyz.stignarnia.shelfly.utilities.deeplink.resolvers

import androidx.core.text.isDigitsOnly
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkResolver
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkSource
import xyz.stignarnia.uiModel.IdTmdb

class TmdbSourceResolver : SourceResolver {
  override fun resolve(linkPath: List<String>): DeepLinkSource? {
    if (linkPath.size < 2 ||
      !(linkPath[0] == DeepLinkResolver.TMDB_TYPE_TV || linkPath[0] == DeepLinkResolver.TMDB_TYPE_MOVIE) ||
      linkPath[1].length <= 1
    ) {
      return null
    }

    val id = linkPath[1].substringBefore("-").trim()
    val type = linkPath[0]
    return if (id.isDigitsOnly()) {
      DeepLinkSource.TmdbSource(IdTmdb(id.toLong()), type)
    } else {
      null
    }
  }
}
