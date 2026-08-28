package xyz.stignarnia.shelfly.ui.main.cases.deeplink

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.uiModel.IdImdb
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

@ViewModelScoped
class MainDeepLinksCase
  @Inject
  constructor(
    private val imdbDeepLinkCase: ImdbDeepLinkCase,
    private val tmdbDeepLinkCase: TmdbDeepLinkCase,
  ) {
    suspend fun findById(imdbId: IdImdb) = imdbDeepLinkCase.findById(imdbId)

    suspend fun findById(
      tmdbId: IdTmdb,
      type: String,
    ) = tmdbDeepLinkCase.findById(tmdbId, type)
  }
