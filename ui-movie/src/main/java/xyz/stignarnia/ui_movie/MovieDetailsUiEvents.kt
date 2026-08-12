@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_movie

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.MovieCollection
import xyz.stignarnia.ui_model.Person
import xyz.stignarnia.ui_people.details.PersonDetailsArgs

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
