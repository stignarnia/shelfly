package xyz.stignarnia.uiPeople.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PeopleRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiModel.Person
import javax.inject.Inject

@ViewModelScoped
class PersonDetailsLoadCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val peopleRepository: PeopleRepository,
    private val dateFormatProvider: DateFormatProvider,
  ) {
    suspend fun loadDetails(person: Person) =
      withContext(dispatchers.IO) {
        peopleRepository
          .loadDetails(person)
          .copy(characters = person.characters)
      }

    fun loadDateFormat() = dateFormatProvider.loadShortDayFormat()
  }
