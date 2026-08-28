package xyz.stignarnia.shelfly.ui.main.cases

import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import javax.inject.Inject

@ViewModelScoped
class MainClearingCase
  @Inject
  constructor(
    private val showImagesProvider: ShowImagesProvider,
    private val movieImagesProvider: MovieImagesProvider,
  ) {
    fun clear() {
      showImagesProvider.clear()
      movieImagesProvider.clear()
      Timber.d("Clearing...")
    }
  }
