package xyz.stignarnia.ui_people.details.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.PeopleRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_model.Person
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class PersonDetailsLoadCase @Inject constructor(
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
