package xyz.stignarnia.uiGallery.fanart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiGallery.fanart.cases.ArtLoadImagesCase
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageFamily
import xyz.stignarnia.uiModel.ImageSource.CUSTOM
import xyz.stignarnia.uiModel.ImageType
import javax.inject.Inject

@HiltViewModel
class ArtGalleryViewModel
  @Inject
  constructor(
    private val imagesCase: ArtLoadImagesCase,
  ) : ViewModel() {
    private val imagesState = MutableStateFlow<List<Image>?>(null)
    private val typeState = MutableStateFlow(ImageType.FANART)
    private val pickedImageState = MutableStateFlow<Event<Image>?>(null)
    private val loadingState = MutableStateFlow(false)

    fun loadImages(
      id: IdTmdb,
      family: ImageFamily,
      type: ImageType,
    ) {
      viewModelScope.launch {
        try {
          loadingState.value = true
          val allImages = imagesCase.loadImages(id, family, type)
          imagesState.value = allImages
          typeState.value = type
          loadingState.value = false
        } catch (t: Throwable) {
          loadingState.value = false
        }
      }
    }

    fun addImageFromUrl(
      imageUrl: String,
      family: ImageFamily,
      type: ImageType,
    ) {
      if (imageUrl.isBlank()) return

      val currentImages = uiState.value.images?.toMutableList() ?: mutableListOf()
      val image = Image.createAvailable(Ids.EMPTY, type, family, imageUrl.trim(), CUSTOM)
      currentImages.add(0, image)

      imagesState.value = currentImages
    }

    val uiState =
      combine(
        imagesState,
        typeState,
        pickedImageState,
        loadingState,
      ) { s1, s2, s3, s4 ->
        ArtGalleryUiState(
          images = s1,
          type = s2,
          pickedImage = s3,
          isLoading = s4,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = ArtGalleryUiState(),
      )
  }
