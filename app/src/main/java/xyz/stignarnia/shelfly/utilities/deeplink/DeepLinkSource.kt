package xyz.stignarnia.shelfly.utilities.deeplink

import xyz.stignarnia.uiModel.IdImdb
import xyz.stignarnia.uiModel.IdTmdb

sealed class DeepLinkSource {
  data class ImdbSource(
    val id: IdImdb,
  ) : DeepLinkSource()

  data class TmdbSource(
    val id: IdTmdb,
    val type: String,
  ) : DeepLinkSource()
}
