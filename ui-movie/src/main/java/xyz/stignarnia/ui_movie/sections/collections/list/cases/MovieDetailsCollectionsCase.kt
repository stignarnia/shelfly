package xyz.stignarnia.ui_movie.sections.collections.list.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.movies.MovieCollectionsRepository
import xyz.stignarnia.repository.movies.MovieCollectionsRepository.Source
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.MovieCollection
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsCollectionsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val repository: MovieCollectionsRepository,
) {

  suspend fun loadMovieCollections(movie: Movie): Pair<List<MovieCollection>, Source> =
    withContext(dispatchers.IO) {
      try {
        val (collections, source) = repository.loadCollections(movie.ids.tmdb)
        return@withContext Pair(
          collections.filter { it.itemCount != -1 },
          source,
        )
      } catch (error: Throwable) {
        return@withContext Pair(
          emptyList(),
          Source.LOCAL,
        )
      }
    }
}
