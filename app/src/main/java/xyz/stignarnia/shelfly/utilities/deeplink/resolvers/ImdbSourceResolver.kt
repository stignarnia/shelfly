package xyz.stignarnia.shelfly.utilities.deeplink.resolvers

import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkSource
import xyz.stignarnia.ui_model.IdImdb

class ImdbSourceResolver : SourceResolver {

  override fun resolve(linkPath: List<String>): DeepLinkSource? {
    if (linkPath.size < 2 || !linkPath[1].startsWith("tt") || linkPath[1].length <= 2) {
      return null
    }

    return DeepLinkSource.ImdbSource(IdImdb(linkPath[1]))
  }
}
