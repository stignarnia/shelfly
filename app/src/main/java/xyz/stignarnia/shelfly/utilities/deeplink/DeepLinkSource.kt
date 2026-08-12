package xyz.stignarnia.shelfly.utilities.deeplink

import xyz.stignarnia.ui_model.IdImdb
import xyz.stignarnia.ui_model.IdTmdb

sealed class DeepLinkSource {

  data class ImdbSource(
    val id: IdImdb,
  ) : DeepLinkSource()

  data class TmdbSource(
    val id: IdTmdb,
    val type: String,
  ) : DeepLinkSource()
}
