package xyz.stignarnia.uiPeople.gallery.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.images.PeopleImagesProvider
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Image
import javax.inject.Inject

@ViewModelScoped
class PersonGalleryImagesCase
  @Inject
  constructor(
    private val imagesProvider: PeopleImagesProvider,
  ) {
    suspend fun loadImages(id: IdTmdb): List<Image> {
      val initial = imagesProvider.loadCachedImage(id)
      val images = imagesProvider.loadImages(id).filter { it.fileUrl != initial?.fileUrl }
      return (listOf(initial) + images).filterNotNull()
    }
  }
