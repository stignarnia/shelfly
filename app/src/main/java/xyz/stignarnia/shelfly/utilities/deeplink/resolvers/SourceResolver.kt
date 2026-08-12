package xyz.stignarnia.shelfly.utilities.deeplink.resolvers

import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkSource

interface SourceResolver {
  fun resolve(linkPath: List<String>): DeepLinkSource?
}
