package xyz.stignarnia.uiSettings.sections.misc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiSettings.R
import xyz.stignarnia.uiSettings.sections.misc.cases.SettingsMiscCacheCase
import javax.inject.Inject

@HiltViewModel
class SettingsMiscViewModel
  @Inject
  constructor(
    private val cacheCase: SettingsMiscCacheCase,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val loadingState = MutableStateFlow(false)

    fun deleteImagesCache(context: Context) {
      viewModelScope.launch {
        withContext(Dispatchers.IO) { Glide.get(context).clearDiskCache() }
        Glide.get(context).clearMemory()
        cacheCase.deleteImagesCache()
        messageChannel.send(MessageEvent.Info(R.string.textImagesCacheCleared))
      }
    }

    val uiState =
      loadingState
        .map { SettingsMiscUiState() }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
          initialValue = SettingsMiscUiState(),
        )
  }
