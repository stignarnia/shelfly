package xyz.stignarnia.uiPeople.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.common.Config
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.findReplace
import xyz.stignarnia.uiBase.utilities.extensions.launchDelayed
import xyz.stignarnia.uiBase.utilities.extensions.replaceItem
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.details.cases.PersonDetailsCreditsCase
import xyz.stignarnia.uiPeople.details.cases.PersonDetailsImagesCase
import xyz.stignarnia.uiPeople.details.cases.PersonDetailsLoadCase
import xyz.stignarnia.uiPeople.details.cases.PersonDetailsTranslationsCase
import xyz.stignarnia.uiPeople.details.filters.PersonDetailsFilters
import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsItem
import javax.inject.Inject

@HiltViewModel
class PersonDetailsViewModel
  @Inject
  constructor(
    private val loadDetailsCase: PersonDetailsLoadCase,
    private val loadCreditsCase: PersonDetailsCreditsCase,
    private val loadImagesCase: PersonDetailsImagesCase,
    private val loadTranslationsCase: PersonDetailsTranslationsCase,
    private val settingsRepository: SettingsRepository,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val personDetailsItemsState = MutableStateFlow<List<PersonDetailsItem>?>(null)

    private var mainProgressJob: Job? = null
    private var creditsJob: Job? = null
    private var creditsProgressJob: Job? = null
    private var imagesJobs = mutableMapOf<String, Boolean>()
    private var translationsJobs = mutableMapOf<String, Boolean>()

    fun loadDetails(
      person: Person,
      personArgs: PersonDetailsArgs,
    ) {
      viewModelScope.launch {
        mainProgressJob = launchDelayed(750) { setMainLoading(true) }
        try {
          val dateFormat = loadDetailsCase.loadDateFormat()
          personDetailsItemsState.value =
            mutableListOf<PersonDetailsItem>().apply {
              add(PersonDetailsItem.MainInfo(person, dateFormat, false))
              if (!person.bio.isNullOrBlank()) {
                add(PersonDetailsItem.MainBio(person.bio, person.bioTranslation))
              }
            }

          val details = loadDetailsCase.loadDetails(person)
          personDetailsItemsState.value =
            mutableListOf<PersonDetailsItem>().apply {
              add(PersonDetailsItem.MainInfo(details, dateFormat, false))
              add(PersonDetailsItem.MainBio(details.bio, details.bioTranslation))
            }
          mainProgressJob?.cancelAndJoin()

          loadCredits(details, personArgs)
        } catch (error: Throwable) {
          messageChannel.send(MessageEvent.Error(R.string.errorGeneral))
          Timber.e(error)
          rethrowCancellation(error)
        } finally {
          setMainLoading(false)
        }
      }
    }

    fun loadCredits(
      person: Person,
      personArgs: PersonDetailsArgs? = null,
      filters: PersonDetailsFilters = PersonDetailsFilters(),
    ) {
      creditsJob?.cancel()
      creditsJob =
        viewModelScope.launch {
          creditsProgressJob = launchDelayed(500) { setCreditsLoading(true) }
          try {
            val credits =
              loadCreditsCase.loadCredits(
                person = person,
                filters = filters,
              )

            setCreditsLoading(false)

            val current = personDetailsItemsState.value?.toMutableList()
            current?.let { currentValue ->
              val filtersItem = PersonDetailsItem.CreditsFiltersItem(filters)
              if (currentValue.none { it is PersonDetailsItem.CreditsFiltersItem }) {
                currentValue.add(filtersItem)
              } else {
                currentValue.findReplace(filtersItem) { it is PersonDetailsItem.CreditsFiltersItem }
              }
              currentValue.removeIf { it.isCreditsItem() }
              credits.forEach { (year, credit) ->
                currentValue.add(PersonDetailsItem.CreditsHeader(year))
                currentValue.addAll(credit)
              }

              personDetailsItemsState.value = currentValue

              personArgs?.let {
                eventChannel.send(
                  PersonDetailsUiEvent.ScrollToPosition(
                    position = it.firstVisibleItemPosition,
                    isSheetExpanded = it.isExpanded,
                    isUpButtonVisible = it.isUpButtonVisible,
                  ),
                )
              }
            }
          } catch (error: Throwable) {
            messageChannel.send(MessageEvent.Error(R.string.errorGeneral))
            Timber.e(error)
            rethrowCancellation(error)
          } finally {
            setCreditsLoading(false)
          }
        }
    }

    fun loadMissingImage(
      item: PersonDetailsItem,
      force: Boolean,
    ) {
      if (item.getId() in imagesJobs.keys) {
        return
      }
      imagesJobs[item.getId()] = true
      viewModelScope.launch {
        (item as? PersonDetailsItem.CreditsShowItem)?.let {
          updateItem(it.copy(isLoading = true))
          val updatedItem = loadImagesCase.loadMissingImage(it, force)
          updateItem(updatedItem)
        }
        (item as? PersonDetailsItem.CreditsMovieItem)?.let {
          updateItem(it.copy(isLoading = true))
          val updatedItem = loadImagesCase.loadMissingImage(it, force)
          updateItem(updatedItem)
        }
      }
    }

    fun loadMissingTranslation(item: PersonDetailsItem) {
      val language = settingsRepository.language
      if (language == Config.DEFAULT_LANGUAGE || item.getId() in translationsJobs.keys) {
        return
      }
      translationsJobs[item.getId()] = true
      viewModelScope.launch {
        (item as? PersonDetailsItem.CreditsShowItem)?.let {
          val updatedItem = loadTranslationsCase.loadMissingTranslation(it, language)
          updateItem(updatedItem)
        }
        (item as? PersonDetailsItem.CreditsMovieItem)?.let {
          val updatedItem = loadTranslationsCase.loadMissingTranslation(it, language)
          updateItem(updatedItem)
        }
      }
    }

    private fun setMainLoading(isLoading: Boolean) {
      if (!isLoading) mainProgressJob?.cancel()

      val current = personDetailsItemsState.value?.toMutableList()
      current?.let { currentValue ->
        val mainInfoItem = currentValue.first { it is PersonDetailsItem.MainInfo } as PersonDetailsItem.MainInfo
        val value = mainInfoItem.copy(isLoading = isLoading)
        currentValue.replaceItem(mainInfoItem, value)
        personDetailsItemsState.value = currentValue
      }
    }

    private fun setCreditsLoading(isLoading: Boolean) {
      if (!isLoading) creditsProgressJob?.cancel()

      val current = personDetailsItemsState.value?.toMutableList()
      current?.let { currentValue ->
        if (isLoading) {
          currentValue.add(PersonDetailsItem.CreditsLoadingItem)
        } else {
          currentValue.remove(PersonDetailsItem.CreditsLoadingItem)
        }
        personDetailsItemsState.value = currentValue
      }
    }

    private fun updateItem(newItem: PersonDetailsItem) {
      val currentItems = personDetailsItemsState.value?.toMutableList()
      currentItems?.findReplace(newItem) { it.getId() == newItem.getId() }
      personDetailsItemsState.value = currentItems
    }

    val uiState =
      combine(
        personDetailsItemsState,
      ) { s1 ->
        PersonDetailsUiState(
          personDetailsItems = s1[0],
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = PersonDetailsUiState(),
      )
  }
