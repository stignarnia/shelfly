package xyz.stignarnia.uiGallery.fanart.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.common.Config.FANART_GALLERY_IMAGES_LIMIT
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageFamily
import xyz.stignarnia.uiModel.ImageFamily.MOVIE
import xyz.stignarnia.uiModel.ImageFamily.SHOW
import xyz.stignarnia.uiModel.ImageStatus.AVAILABLE
import xyz.stignarnia.uiModel.ImageType
import javax.inject.Inject

@ViewModelScoped
class ArtLoadImagesCase
  @Inject
  constructor(
    private val showsRepository: ShowsRepository,
    private val moviesRepository: MoviesRepository,
    private val showImagesProvider: ShowImagesProvider,
    private val movieImagesProvider: MovieImagesProvider,
  ) {
    suspend fun loadImages(
      id: IdTmdb,
      family: ImageFamily,
      type: ImageType,
    ): List<Image> {
      val images = mutableListOf<Image>()
      val initialImage = loadInitialImage(id, family, type)
      if (initialImage.status == AVAILABLE) {
        images.add(initialImage)
      }

      var remoteImages: List<Image> = emptyList()
      if (family == SHOW) {
        val show = showsRepository.detailsShow.load(id)
        remoteImages = showImagesProvider.loadRemoteImages(show, type)
      } else if (family == MOVIE) {
        val movie = moviesRepository.movieDetails.load(id)
        remoteImages = movieImagesProvider.loadRemoteImages(movie, type)
      }
      images.addAll(remoteImages.filter { it.fullFileUrl != initialImage.fullFileUrl })
      return images.take(FANART_GALLERY_IMAGES_LIMIT)
    }

    private suspend fun loadInitialImage(
      id: IdTmdb,
      family: ImageFamily,
      type: ImageType,
    ) = when (family) {
      SHOW -> {
        val show = showsRepository.detailsShow.load(id)
        showImagesProvider.findCachedImage(show, type)
      }

      MOVIE -> {
        val movie = moviesRepository.movieDetails.load(id)
        movieImagesProvider.findCachedImage(movie, type)
      }

      else -> {
        throw IllegalStateException()
      }
    }
  }
