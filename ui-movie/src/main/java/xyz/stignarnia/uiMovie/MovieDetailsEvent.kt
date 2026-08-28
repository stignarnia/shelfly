
package xyz.stignarnia.uiMovie

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.MovieCollection
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiPeople.details.PersonDetailsArgs

sealed class MovieDetailsEvent<T>(
  action: T,
) : Event<T>(action) {
  data class OpenPersonSheet(
    val movie: Movie,
    val person: Person,
    val personArgs: PersonDetailsArgs?,
  ) : MovieDetailsEvent<Movie>(movie)

  data class OpenPeopleSheet(
    val movie: Movie,
    val people: List<Person>,
    val department: Person.Department,
  ) : MovieDetailsEvent<Movie>(movie)

  data class OpenCollectionSheet(
    val movie: Movie,
    val collection: MovieCollection,
  ) : MovieDetailsEvent<Movie>(movie)

  data class OpenDateSelectionSheet(
    val movie: Movie,
  ) : MovieDetailsEvent<Movie>(movie)

  object RequestWidgetsUpdate : MovieDetailsEvent<Unit>(Unit)

  object Finish : MovieDetailsEvent<Unit>(Unit)
}
