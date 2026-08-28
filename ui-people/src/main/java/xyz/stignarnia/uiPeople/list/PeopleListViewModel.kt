package xyz.stignarnia.uiPeople.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.common.Mode
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiPeople.list.cases.PeopleListItemsCase
import xyz.stignarnia.uiPeople.list.recycler.PeopleListItem
import javax.inject.Inject

@HiltViewModel
class PeopleListViewModel
  @Inject
  constructor(
    private val itemsCase: PeopleListItemsCase,
  ) : ViewModel() {
    private val peopleListState = MutableStateFlow<List<PeopleListItem>?>(null)

    fun loadPeople(
      idTmdb: IdTmdb,
      title: String,
      mode: Mode,
      department: Person.Department,
    ) {
      viewModelScope.launch {
        val header = PeopleListItem.HeaderItem(department, title)
        val people = itemsCase.loadPeople(idTmdb, mode, department)
        peopleListState.value = listOf(header) + people
      }
    }

    val uiState =
      combine(
        peopleListState,
      ) { s1 ->
        PeopleListUiState(
          peopleItems = s1[0],
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = PeopleListUiState(),
      )
  }
